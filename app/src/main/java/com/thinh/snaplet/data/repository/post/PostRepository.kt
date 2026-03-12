package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.model.post.SseEvent
import com.thinh.snaplet.data.model.post.UnreadCountData
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observePostStream(): Flow<SseEvent>
    suspend fun getUnreadCount(): ApiResult<UnreadCountData>
    suspend fun markSeen(): ApiResult<Unit>
}