package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.FeedData
import com.thinh.snaplet.data.model.StandardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    
    @GET("posts/feed")
    suspend fun getMediaFeed(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<StandardResponse<FeedData>>
}
