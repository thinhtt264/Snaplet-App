package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.MediaItem
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service
 * Defines all API endpoints
 * 
 * Simplified approach: Return domain models directly
 * No DTO layer for small apps
 */
interface ApiService {
    
    /**
     * Get media feed
     * GET /api/media/feed
     */
    @GET("media/feed")
    suspend fun getMediaFeed(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<List<MediaItem>>
    
    /**
     * Get single media item by ID
     */
    @GET("media/{id}")
    suspend fun getMediaById(
        @Path("id") mediaId: String
    ): Response<MediaItem>
    
    /**
     * Delete media item
     */
    @DELETE("media/{id}")
    suspend fun deleteMedia(
        @Path("id") mediaId: String
    ): Response<Unit>
}
