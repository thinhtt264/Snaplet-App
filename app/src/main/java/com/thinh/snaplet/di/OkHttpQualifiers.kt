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
 * Logging Interceptor: custom HTTP logger (method, url, headers, body, status).
 * Distinguishes from AuthInterceptor which also provides okhttp3.Interceptor.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LoggingInterceptor