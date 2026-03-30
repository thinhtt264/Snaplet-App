package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.post.MarkPostsSeenRequest
import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.data.model.post.PostActivity
import com.thinh.snaplet.data.model.post.PostsFeedData
import com.thinh.snaplet.data.model.post.PostReactionUser
import com.thinh.snaplet.data.model.post.UnreadPostsCountData
import com.thinh.snaplet.platform.socket.SocketEvent
import com.thinh.snaplet.platform.socket.SocketManager
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.GsonHolder.gson
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val socketManager: SocketManager,
    private val apiService: ApiService,
) : PostRepository {

    override val newPostMessages: Flow<NewPostUpdate> =
        socketManager.messages
            .filter { it.event == SocketEvent.POSTS_UNREAD_UPDATED }
            .mapNotNull { message ->
                val jsonString = message.args ?: return@mapNotNull null
                runCatching {
                    gson.fromJson(jsonString, NewPostUpdate::class.java)
                }.onFailure {
                    Logger.e("PostRepository: parse posts_unread_updated failed: ${it.message}")
                }.getOrNull()
            }

    override suspend fun getUnreadPostsCount(): ApiResult<Int> {
        return safeApiCall(
            apiCall = { apiService.getUnreadPostsCount() },
            transform = { data: UnreadPostsCountData -> data.count }
        )
    }

    override suspend fun getNewsfeed(limit: Int, cursor: String?): ApiResult<PostsFeedData> {
        return safeApiCall(
            apiCall = { apiService.getPostsFeed(limit = limit, cursor = cursor) },
        )
    }

    override suspend fun getNewerPost(since: String, limit: Int): ApiResult<List<Post>> {
        return safeApiCall(
            apiCall = { apiService.getNewerFeed(since = since, limit = limit) },
        )
    }

    override suspend fun getPostsActivity(): ApiResult<PostActivity?> {
        return safeApiCall(
            apiCall = { apiService.getPostsActivity() },
        )
    }

    override suspend fun getPostReactions(
        postId: String,
    ): ApiResult<List<PostReactionUser>> {
        return safeApiCall(
            apiCall = { apiService.getPostReactions(postId = postId) },
        )
    }

    override suspend fun markPostsSeen(
        lastSeenPostCreatedAt: Date,
    ): ApiResult<Unit> {
        val body = MarkPostsSeenRequest(
            lastSeenPostCreatedAt = lastSeenPostCreatedAt,
        )
        return safeApiCall(
            apiCall = { apiService.markPostsSeen(body) }
        )
    }
}