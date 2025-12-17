package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.FeedData
import com.thinh.snaplet.data.model.MediaItem
import com.thinh.snaplet.data.model.StandardResponse
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
     * Get media feed from posts endpoint
     * GET /posts/feed
     */
    @GET("posts/feed")
    suspend fun getMediaFeed(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<StandardResponse<FeedData>>
    
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
