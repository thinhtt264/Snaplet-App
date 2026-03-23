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
import com.thinh.snaplet.network.SessionController
import com.thinh.snaplet.network.TokenAuthenticator
import com.thinh.snaplet.network.TokenRefreshCoordinator
import com.thinh.snaplet.platform.socket.SocketConfig
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.UtcDateDeserializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val BASE_URL = if (BuildConfig.DEBUG) "https://api-stg.snaplet.site/api/v1/"
    else "https://api-stg.snaplet.site/api/v1/"

    private val SOCKET_BASE_URL = if (BuildConfig.DEBUG) "https://api-stg.snaplet.site"
    else "https://api-stg.snaplet.site"

    @Provides
    @Singleton
    fun provideSocketConfig(): SocketConfig = SocketConfig(baseUrl = SOCKET_BASE_URL)

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().serializeNulls() // Include null fields in JSON
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX") // ISO 8601 with timezone offset/Z
            .registerTypeAdapter(Date::class.java, UtcDateDeserializer())
            .create()
    }

    /**
     * Provide HTTP Logging Interceptor
     * Logs method, url, request headers/body, and response status — debug only.
     */
    @Provides
    @Singleton
    @LoggingInterceptor
    fun provideLoggingInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()

            if (!BuildConfig.DEBUG) return@Interceptor chain.proceed(request)

            val reqBody = request.body?.let { body ->
                val buffer = okio.Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }

            Logger.d(buildString {
                appendLine("┌── REQUEST ─────────────────────────────")
                appendLine("│ ${request.method} ${request.url}")
                request.headers.forEach { (name, value) ->
                    if (name.lowercase() !in listOf("accept-encoding", "connection")) {
                        appendLine("│ $name: $value")
                    }
                }
                if (!reqBody.isNullOrBlank()) appendLine("│ Body: $reqBody")
                append("└────────────────────────────────────────")
            })

            val startMs = System.currentTimeMillis()
            val response = chain.proceed(request)
            val durationMs = System.currentTimeMillis() - startMs
            val responseBody = response.peekBody(Long.MAX_VALUE).string()

            Logger.d(buildString {
                appendLine("┌── RESPONSE ────────────────────────────")
                appendLine("│ ${response.code} ${request.method} ${request.url} (${durationMs}ms)")
                appendLine("│ Body: $responseBody")
                append("└────────────────────────────────────────")
            })

            response
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
     * Session latch reset after login/register; same instance as [TokenRefreshCoordinator].
     */
    @Provides
    @Singleton
    fun provideSessionController(
        tokenRefreshCoordinator: TokenRefreshCoordinator,
    ): SessionController = tokenRefreshCoordinator

    /**
     * Base OkHttpClient: basic, default configuration (timeouts, retry).
     * Used for upload/download – no auth or API interceptors.
     */
    @Provides
    @Singleton
    @BaseOkHttpClient
    fun provideBaseOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Internal OkHttpClient: custom internal config for backend
     * Used for Retrofit – API calls to backend.
     */
    @Provides
    @Singleton
    @InternalOkHttpClient
    fun provideInternalOkHttpClient(
        @LoggingInterceptor loggingInterceptor: Interceptor,
        authInterceptor: Interceptor,
        fingerprintInterceptor: FingerprintInterceptor,
        chuckerInterceptor: ChuckerInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(fingerprintInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(chuckerInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(
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

    @Provides
    @Singleton
    fun provideRetrofit(
        @InternalOkHttpClient okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson)).build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}