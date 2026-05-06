package com.thinh.snaplet.di

import javax.inject.Qualifier

/**
 * Base OkHttpClient: basic, default configuration (timeouts, retry).
 * Used for upload/download (signed URLs, CDN/S3) – no auth or API interceptors.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseOkHttpClient

/**
 * Internal OkHttpClient: custom internal config for backend
 * Used for Retrofit – API calls to backend.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InternalOkHttpClient

/**
 * Auth OkHttpClient: dedicated client for authentication-related endpoints.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthOkHttpClient

/**
 * ApiService backed by [AuthOkHttpClient].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthApiService

/**
 * Logging Interceptor: custom HTTP logger (method, url, headers, body, status).
 * Distinguishes from AuthInterceptor which also provides okhttp3.Interceptor.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LoggingInterceptor