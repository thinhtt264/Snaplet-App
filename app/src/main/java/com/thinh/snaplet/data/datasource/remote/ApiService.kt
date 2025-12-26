package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.BaseResponse
import com.thinh.snaplet.data.model.FeedData
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    
    @GET("posts/feed")
    suspend fun getMediaFeed(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<BaseResponse<FeedData>>
    
    @GET("users/profile/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String
    ): Response<BaseResponse<UserProfile>>
    
    @POST("relationships")
    suspend fun sendFriendRequest(
        @Body body: Map<String, String>
    ): Response<BaseResponse<Relationship>>
}
