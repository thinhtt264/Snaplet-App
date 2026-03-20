package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.PostsFeedData
import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.utils.network.ApiResult
import java.util.Date
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    val newPostMessages: Flow<NewPostUpdate>

    suspend fun getUnreadPostsCount(): ApiResult<Int>

    suspend fun getNewsfeed(limit: Int = 5, cursor: String? = null): ApiResult<PostsFeedData>

    suspend fun getNewerPost(since: String, limit: Int): ApiResult<List<Post>>

    suspend fun markPostsSeen(
        lastSeenPostCreatedAt: Date,
    ): ApiResult<Unit>
}
