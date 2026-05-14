package com.thinh.snaplet.data.datasource.remote

import com.thinh.snaplet.data.model.BaseResponse
import com.thinh.snaplet.data.model.CompleteOnboardRequest
import com.thinh.snaplet.data.model.EmailAvailabilityData
import com.thinh.snaplet.data.model.LoginRequest
import com.thinh.snaplet.data.model.LoginResponse
import com.thinh.snaplet.data.model.LoginWithGoogleRequest
import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.RefreshTokenRequest
import com.thinh.snaplet.data.model.RegisterRequest
import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipCounts
import com.thinh.snaplet.data.model.RelationshipWithUserDto
import com.thinh.snaplet.data.model.TokenResponse
import com.thinh.snaplet.data.model.UpdateRelationshipRequest
import com.thinh.snaplet.data.model.UsernameAvailabilityData
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.ConversationLookupResult
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageReaction
import com.thinh.snaplet.data.model.chat.MessageReactionWithUserInfo
import com.thinh.snaplet.data.model.chat.ReactToMessageRequest
import com.thinh.snaplet.data.model.chat.SendMessageRequest
import com.thinh.snaplet.data.model.media.ConfirmUploadData
import com.thinh.snaplet.data.model.media.MediaConfirmUploadRequest
import com.thinh.snaplet.data.model.media.RequestUploadRequest
import com.thinh.snaplet.data.model.media.UploadRequestData
import com.thinh.snaplet.data.model.post.CreatePostRequest
import com.thinh.snaplet.data.model.post.MarkPostsSeenRequest
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.data.model.post.PostActivity
import com.thinh.snaplet.data.model.post.PostReactionUser
import com.thinh.snaplet.data.model.post.PostsFeedData
import com.thinh.snaplet.data.model.post.ReactToPostRequest
import com.thinh.snaplet.data.model.post.ReactToPostResponse
import com.thinh.snaplet.data.model.post.UnreadPostsCountData
import com.thinh.snaplet.data.model.user.AvatarUploadRequest
import com.thinh.snaplet.data.model.user.AvatarUploadRequestResponse
import com.thinh.snaplet.data.model.user.ConfirmAvatarUploadRequest
import com.thinh.snaplet.data.model.user.UpdateDisplayNameRequest
import com.thinh.snaplet.data.model.user.UpdateFcmTokenRequest
import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.data.model.user.UserSearchResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): Response<BaseResponse<LoginResponse>>

    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body body: LoginWithGoogleRequest
    ): Response<BaseResponse<LoginResponse>>

    @POST("auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): Response<BaseResponse<LoginResponse>>

    @GET("posts/feed")
    suspend fun getPostsFeed(
        @Query("limit") limit: Int = 10,
        @Query("cursor") cursor: String? = null,
        @Query("userId") userId: String? = null
    ): Response<BaseResponse<PostsFeedData>>

    @GET("posts/feed/newer")
    suspend fun getNewerFeed(
        @Query("since") since: String,
        @Query("limit") limit: Int,
    ): Response<BaseResponse<List<Post>>>

    @GET("posts/activity")
    suspend fun getPostsActivity(): Response<BaseResponse<PostActivity?>>

    @GET("users/profile/{username}")
    suspend fun getUserProfile(
        @Path("username") username: String
    ): Response<BaseResponse<UserProfile>>

    @PATCH("users/me/complete-onboarding")
    suspend fun completeOnboarding(
        @Body body: CompleteOnboardRequest
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
        @Path("relationshipId") relationshipId: String, @Body body: UpdateRelationshipRequest
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
    suspend fun logout(
        @Header("Authorization") authorization: String
    ): Response<BaseResponse<Unit>>

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

    @PATCH("users/me/fcm-token")
    suspend fun updateFcmToken(
        @Body body: UpdateFcmTokenRequest
    ): Response<BaseResponse<Unit>>

    @POST("posts")
    suspend fun createPost(
        @Body body: CreatePostRequest
    ): Response<BaseResponse<Post>>

    @DELETE("posts/{postId}")
    suspend fun deletePost(
        @Path("postId") postId: String
    ): Response<BaseResponse<Unit>>

    @GET("posts/{postId}")
    suspend fun getPostById(
        @Path("postId") postId: String
    ): Response<BaseResponse<Post>>

    @GET("posts/unread-count")
    suspend fun getUnreadPostsCount(
    ): Response<BaseResponse<UnreadPostsCountData>>

    @POST("posts/mark-seen")
    suspend fun markPostsSeen(
        @Body body: MarkPostsSeenRequest
    ): Response<BaseResponse<Unit>>

    @GET("posts/{postId}/reactions")
    suspend fun getPostReactions(
        @Path("postId") postId: String
    ): Response<BaseResponse<List<PostReactionUser>>>

    @PATCH("posts/{postId}/reactions")
    suspend fun reactToPost(
        @Path("postId") postId: String,
        @Body body: ReactToPostRequest,
    ): Response<BaseResponse<ReactToPostResponse>>

    @PATCH("posts/{postId}/owner-viewed")
    suspend fun markPostOwnerViewed(
        @Path("postId") postId: String,
    ): Response<BaseResponse<Unit>>

    // ── Chat ──────────────────────────────────────────────────────────────

    @POST("messages")
    suspend fun sendMessage(
        @Body body: SendMessageRequest
    ): Response<BaseResponse<Message>>

    @GET("conversations")
    suspend fun getConversations(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<BaseResponse<PaginatedResponse<Conversation>>>

    @GET("conversations/{conversationId}")
    suspend fun getConversationById(
        @Path("conversationId") conversationId: String
    ): Response<BaseResponse<Conversation>>

    @GET("conversations/lookup/id")
    suspend fun lookupConversationId(
        @Query("targetUserId") targetUserId: String,
    ): Response<BaseResponse<ConversationLookupResult>>

    @PATCH("conversations/{conversationId}/messages/{messageId}/seen")
    suspend fun markMessageSeen(
        @Path("conversationId") conversationId: String,
        @Path("messageId") messageId: String,
    ): Response<BaseResponse<Unit>>

    @GET("conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): Response<BaseResponse<PaginatedResponse<Message>>>

    @POST("messages/{messageId}/reactions")
    suspend fun reactToMessage(
        @Path("messageId") messageId: String,
        @Body body: ReactToMessageRequest,
    ): Response<BaseResponse<List<MessageReaction>>>

    @GET("messages/{messageId}/reactions")
    suspend fun getMessageReactions(
        @Path("messageId") messageId: String,
    ): Response<BaseResponse<List<MessageReactionWithUserInfo>>>
}
