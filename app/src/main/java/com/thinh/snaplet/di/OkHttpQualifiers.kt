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
 * Stream OkHttpClient: long‑lived connections (SSE, websockets, etc.).
 * Reuses base client but adds auth + fingerprint, without Chucker.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StreamOkHttpClient

/**
 * Base URL for backend API.
 * Used to build absolute URLs for features that cannot use Retrofit (e.g. SSE).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl
