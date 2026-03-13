package com.thinh.snaplet.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thinh.snaplet.BuildConfig
import com.thinh.snaplet.data.datasource.local.datastore.DataStoreManager
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.network.FingerprintInterceptor
import com.thinh.snaplet.network.TokenAuthenticator
import com.thinh.snaplet.network.TokenRefreshCoordinator
import com.thinh.snaplet.utils.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = if (BuildConfig.DEBUG) "http://10.0.2.2:4040/api/v1/"
    else "https://api-stg.snaplet.site/api/v1/"

    @Provides
    @Singleton
    @ApiBaseUrl
    fun provideApiBaseUrl(): String = BASE_URL

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().serializeNulls() // Include null fields in JSON
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // ISO 8601
            .create()
    }

    /**
     * Provide HTTP Logging Interceptor Logs all HTTP requests/responses for
     * debugging
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Logger.d("🌐 HTTP: $message")
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY // Full logs in debug
            } else {
                HttpLoggingInterceptor.Level.BASIC // Minimal logs in release
            }
        }
    }

    /**
     * Provide Authentication Interceptor Adds auth token to all requests from
     * cache (non-blocking)
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        dataStoreManager: DataStoreManager
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            val token = dataStoreManager.getAccessToken()

            val requestBuilder =
                originalRequest.newBuilder().addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")

            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }

            val newRequest = requestBuilder.build()

            chain.proceed(newRequest)
        }
    }

    /**
     * Provide Chucker Interceptor for network debugging In debug builds:
     * full network inspector In release builds: no-op (from library-no-op
     * dependency)
     */
    @Provides
    @Singleton
    fun provideChuckerInterceptor(
        @ApplicationContext context: Context
    ): ChuckerInterceptor {
        val chuckerCollector = ChuckerCollector(
            context = context,
            showNotification = false,
            retentionPeriod = RetentionManager.Period.ONE_HOUR
        )
        return ChuckerInterceptor.Builder(context).collector(chuckerCollector)
            .maxContentLength(250000L) // 250KB
            .redactHeaders("Authorization", "Cookie")
            .alwaysReadResponseBody(true) // Read response body even if it's large
            .createShortcut(false).build()
    }

    /**
     * Provide TokenRefreshCoordinator Manages token refresh operations with
     * mutex lock and request cancellation
     */
    @Provides
    @Singleton
    fun provideTokenRefreshCoordinator(
        authRepository: dagger.Lazy<com.thinh.snaplet.data.repository.auth.AuthRepository>
    ): TokenRefreshCoordinator {
        return TokenRefreshCoordinator(authRepository)
    }

    /**
     * Provide TokenAuthenticator Handles 401 responses and token refresh using
     * OkHttp Authenticator pattern
     */
    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenRefreshCoordinator: TokenRefreshCoordinator,
        authRepository: dagger.Lazy<com.thinh.snaplet.data.repository.auth.AuthRepository>
    ): TokenAuthenticator {
        return TokenAuthenticator(tokenRefreshCoordinator, authRepository)
    }

    /**
     * Base OkHttpClient: basic, default configuration (timeouts, retry).
     * Used for upload/download – no auth or API interceptors.
     */
    @Provides
    @Singleton
    @BaseOkHttpClient
    fun provideBaseOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true).build()
    }

    /**
     * Internal OkHttpClient: custom internal config for backend
     * Used for Retrofit – API calls to backend.
     */
    @Provides
    @Singleton
    @InternalOkHttpClient
    fun provideInternalOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        fingerprintInterceptor: FingerprintInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val builder = OkHttpClient.Builder().addInterceptor(fingerprintInterceptor)
            .addInterceptor(authInterceptor).addInterceptor(chuckerInterceptor)
            .addInterceptor(loggingInterceptor).authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(true).connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5, keepAliveDuration = 5, timeUnit = TimeUnit.MINUTES
                )
            )
        // Optional: SSL Certificate Pinning for security
        // .certificatePinner(
        //     CertificatePinner.Builder()
        //         .add("api.snaplet.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        //         .build()
        // )

        return builder.build()
    }

    /**
     * Stream OkHttpClient: long‑lived connections such as SSE.
     * Reuses base client, adds auth + fingerprint, but intentionally skips Chucker.
     */
    @Provides
    @Singleton
    @StreamOkHttpClient
    fun provideStreamOkHttpClient(
        @BaseOkHttpClient baseClient: OkHttpClient,
        dataStoreManager: DataStoreManager,
        fingerprintInterceptor: FingerprintInterceptor,
//        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val streamAuthInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = dataStoreManager.getAccessToken()
            val builder = original.newBuilder()
            builder.header("Authorization", "Bearer $token")
            chain.proceed(builder.build())
        }
        return baseClient.newBuilder()
//            .authenticator(tokenAuthenticator)
            .connectionPool(ConnectionPool(2, 3, TimeUnit.MINUTES))
            .addInterceptor(fingerprintInterceptor)
            .addInterceptor(streamAuthInterceptor)
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .connectTimeout(30, TimeUnit.SECONDS).build()
    }

    /** Retrofit dùng Internal OkHttpClient (BE). */
    @Provides
    @Singleton
    fun provideRetrofit(
        @InternalOkHttpClient okHttpClient: OkHttpClient, gson: Gson
    ): Retrofit {
        return Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }

    /** Provide ApiService Retrofit interface implementation */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}