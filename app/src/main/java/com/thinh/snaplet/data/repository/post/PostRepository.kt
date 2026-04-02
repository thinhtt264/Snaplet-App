package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.data.model.post.PostActivity
import com.thinh.snaplet.data.model.post.PostReactionUser
import com.thinh.snaplet.data.model.post.PostsFeedData
import com.thinh.snaplet.data.model.post.ReactToPostResponse
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PostRepository {
    val newPostMessages: Flow<NewPostUpdate>

    suspend fun getUnreadPostsCount(): ApiResult<Int>

    suspend fun getNewsfeed(
        limit: Int = 5,
        cursor: String? = null,
        userId: String? = null
    ): ApiResult<PostsFeedData>

    suspend fun getNewerPost(since: String, limit: Int): ApiResult<List<Post>>

    suspend fun getPostsActivity(): ApiResult<PostActivity?>

    suspend fun getPostReactions(
        postId: String,
    ): ApiResult<List<PostReactionUser>>

    suspend fun reactToPost(
        postId: String,
        reactionIcon: String,
    ): ApiResult<ReactToPostResponse>

    suspend fun markPostsSeen(
        lastSeenPostCreatedAt: Date,
    ): ApiResult<Unit>

    suspend fun getQuickChatRecentEmojis(): List<String>

    suspend fun recordQuickChatEmojiUsage(emoji: String)
}
