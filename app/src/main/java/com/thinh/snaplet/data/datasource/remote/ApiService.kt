package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.BaseResponse
import com.thinh.snaplet.data.model.EmailAvailabilityData
import com.thinh.snaplet.data.model.RelationshipCounts
import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.LoginResponse
import com.thinh.snaplet.data.model.RefreshTokenRequest
import com.thinh.snaplet.data.model.RegisterRequest
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipWithUserDto
import com.thinh.snaplet.data.model.TokenResponse
import com.thinh.snaplet.data.model.UpdateRelationshipRequest
import com.thinh.snaplet.data.model.UsernameAvailabilityData
import com.thinh.snaplet.data.model.media.ConfirmUploadData
import com.thinh.snaplet.data.model.media.MediaConfirmUploadRequest
import com.thinh.snaplet.data.model.media.RequestUploadRequest
import com.thinh.snaplet.data.model.media.UploadRequestData
import com.thinh.snaplet.data.model.post.CreatePostRequest
import com.thinh.snaplet.data.model.post.MarkPostsSeenRequest
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.data.model.post.PostsFeedData
import com.thinh.snaplet.data.model.post.UnreadPostsCountData
import com.thinh.snaplet.data.model.user.AvatarUploadRequest
import com.thinh.snaplet.data.model.user.AvatarUploadRequestResponse
import com.thinh.snaplet.data.model.user.ConfirmAvatarUploadRequest
import com.thinh.snaplet.data.model.user.UpdateDisplayNameRequest
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.data.model.user.UserSearchResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<BaseResponse<LoginResponse>>

    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<BaseResponse<LoginResponse>>

    @GET("posts/feed")
    suspend fun getPostsFeed(
        @Query("limit") limit: Int = 10,
        @Query("cursor") cursor: String? = null
    ): Response<BaseResponse<PostsFeedData>>

    @GET("posts/feed/newer")
    suspend fun getNewerFeed(
        @Query("since") since: String,
        @Query("limit") limit: Int,
    ): Response<BaseResponse<List<Post>>>

    @GET("users/profile/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String
    ): Response<BaseResponse<UserProfile>>

    @GET("users/search")
    suspend fun searchUsersByUsernamePrefix(
        @Query("q") usernamePrefix: String,
        @Query("limit") limit: Int,
    ): Response<BaseResponse<List<UserSearchResult>>>

    @POST("relationships")
    suspend fun sendFriendRequest(
        @Body body: Map<String, String>
    ): Response<BaseResponse<Relationship>>

    @POST("relationships/with-user")
    suspend fun getRelationshipWithUser(
        @Body body: Map<String, String>
    ): Response<BaseResponse<Relationship?>>

    @GET("relationships/count")
    suspend fun getRelationshipCounts(): Response<BaseResponse<RelationshipCounts>>

    @GET("relationships")
    suspend fun getRelationshipsByStatus(
        @Query("statuses") statuses: String
    ): Response<BaseResponse<List<RelationshipWithUserDto>>>

    @PATCH("relationships/{relationshipId}")
    suspend fun updateRelationship(
        @Path("relationshipId") relationshipId: String,
        @Body body: UpdateRelationshipRequest
    ): Response<BaseResponse<Relationship>>

    @DELETE("relationships/{relationshipId}")
    suspend fun removeRelationship(
        @Path("relationshipId") relationshipId: String
    ): Response<BaseResponse<Unit>>

    @GET("users/email-availability")
    suspend fun checkEmailAvailability(
        @Query("email") email: String
    ): Response<BaseResponse<EmailAvailabilityData>>

    @GET("users/username-availability")
    suspend fun checkUsernameAvailability(
        @Query("username") username: String
    ): Response<BaseResponse<UsernameAvailabilityData>>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body body: RefreshTokenRequest
    ): Response<BaseResponse<TokenResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<BaseResponse<Unit>>

    @POST("media/upload/request")
    suspend fun requestUpload(
        @Body body: RequestUploadRequest
    ): Response<BaseResponse<UploadRequestData>>

    @POST("media/upload/confirm")
    suspend fun confirmUpload(
        @Body body: MediaConfirmUploadRequest
    ): Response<BaseResponse<ConfirmUploadData>>

    @POST("users/avatar/upload/request")
    suspend fun requestAvatarUpload(
        @Body body: AvatarUploadRequest
    ): Response<BaseResponse<AvatarUploadRequestResponse>>

    @POST("users/avatar/upload/confirm")
    suspend fun confirmAvatarUpload(
        @Body body: ConfirmAvatarUploadRequest
    ): Response<BaseResponse<UserProfile>>

    @DELETE("users/avatar")
    suspend fun deleteAvatar(
    ): Response<BaseResponse<UserProfile>>

    @PATCH("users/display-name")
    suspend fun updateDisplayName(
        @Body body: UpdateDisplayNameRequest
    ): Response<BaseResponse<UserProfile>>

    @POST("posts")
    suspend fun createPost(
        @Body body: CreatePostRequest
    ): Response<BaseResponse<Post>>

    @DELETE("posts/{postId}")
    suspend fun deletePost(
        @Path("postId") postId: String
    ): Response<BaseResponse<Unit>>

    @GET("posts/unread-count")
    suspend fun getUnreadPostsCount(
    ): Response<BaseResponse<UnreadPostsCountData>>

    @POST("posts/mark-seen")
    suspend fun markPostsSeen(
        @Body body: MarkPostsSeenRequest
    ): Response<BaseResponse<Unit>>
}
