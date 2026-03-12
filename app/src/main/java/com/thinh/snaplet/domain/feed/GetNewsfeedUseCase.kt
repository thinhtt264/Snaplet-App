package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.post.PostsFeedData
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

/**
 * Loads the newsfeed (initial or paginated).
 * ViewModel decides when to call (initial load vs load more).
 */
class GetNewsfeedUseCase @Inject constructor(
    private val mediaRepository: MediaRepository
) {
    suspend operator fun invoke(limit: Int = 5, cursor: String? = null): ApiResult<PostsFeedData> {
        return mediaRepository.getNewsfeed(limit = limit, cursor = cursor)
    }
}
