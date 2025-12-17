package com.thinh.snaplet.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thinh.snaplet.BuildConfig
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.utils.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network Module for Dependency Injection
 * Provides Retrofit, OkHttp, and related dependencies
 * 
 * Production-ready configuration with:
 * - Logging interceptor
 * - Authentication interceptor
 * - Timeout configuration
 * - Error handling
 * - SSL pinning (optional)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"
    
    // private val BASE_URL = when (BuildConfig.BUILD_TYPE) {
    //     "debug" -> "https://api-dev.snaplet.com/v1/"
    //     "staging" -> "https://api-staging.snaplet.com/v1/"
    //     else -> "https://api.snaplet.com/v1/"
    // }
    
    /**
     * Provide Gson instance
     * Configure JSON parsing behavior
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .serializeNulls() // Include null fields in JSON
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // ISO 8601
            .create()
    }
    
    /**
     * Provide HTTP Logging Interceptor
     * Logs all HTTP requests/responses for debugging
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
     * Provide Authentication Interceptor
     * Adds auth token to all requests
     */
    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            
            // Get auth token (from SharedPreferences, DataStore, etc.)
            // val token = authManager.getToken()
            val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiI2OTQyMmYzOTI0NTUxYjQyZWM4Y2IzYjgiLCJpYXQiOjE3NjU5NjE1NjAsImV4cCI6MTc2NjU2NjM2MH0.5guoIpdvsuV_CFv6AhWpwtRNuePNPc3L1QIlECJrFfs"
            
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
            
            chain.proceed(newRequest)
        }
    }
    
    /**
     * Provide OkHttpClient
     * Configures HTTP client with interceptors and timeouts
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // Interceptors
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            
            // Timeouts
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            
            // Retry configuration
            .retryOnConnectionFailure(true)
            
            // Connection pool
            .connectionPool(
                okhttp3.ConnectionPool(
                    maxIdleConnections = 5,
                    keepAliveDuration = 5,
                    timeUnit = TimeUnit.MINUTES
                )
            )
            
            // Optional: SSL Certificate Pinning for security
            // .certificatePinner(
            //     CertificatePinner.Builder()
            //         .add("api.snaplet.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            //         .build()
            // )
            
            .build()
    }
    
    /**
     * Provide Retrofit instance
     * Main HTTP client for API calls
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    /**
     * Provide ApiService
     * Retrofit interface implementation
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
