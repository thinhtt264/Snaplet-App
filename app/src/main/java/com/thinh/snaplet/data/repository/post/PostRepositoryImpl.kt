package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.post.SseEvent
import com.thinh.snaplet.data.model.post.UnreadCountData
import com.thinh.snaplet.di.ApiBaseUrl
import com.thinh.snaplet.network.SseClient
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val sseClient: SseClient,
    @ApiBaseUrl private val apiBaseUrl: String,
) : PostRepository {

    override fun observePostStream(): Flow<SseEvent> {
        val url = apiBaseUrl + ApiService.POSTS_STREAM_PATH
        return sseClient.connect(url)
    }

    override suspend fun getUnreadCount(): ApiResult<UnreadCountData> {
        return safeApiCall(
            apiCall = { apiService.getUnreadCount() },
        )
    }

    override suspend fun markSeen(): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.markSeen() },
        )
    }
}