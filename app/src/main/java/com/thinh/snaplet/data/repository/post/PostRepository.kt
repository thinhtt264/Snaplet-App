package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.utils.network.ApiResult
import java.util.Date
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    val newPostMessages: Flow<NewPostUpdate>

    suspend fun getUnreadPostsCount(): ApiResult<Int>

    suspend fun markPostsSeen(
        lastSeenPostCreatedAt: Date,
    ): ApiResult<Unit>
}
