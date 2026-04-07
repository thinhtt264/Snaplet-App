package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.post.PostsFeedData
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class GetNewsfeedUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    companion object {
        const val FEED_PAGE_LIMIT = 5
        const val GRID_MIN_POST_COUNT = 15
        const val GRID_LOAD_MORE_STEP = 10
        const val GRID_TRIGGER_FROM_BOTTOM_RATIO = 0.3f
    }

    fun gridInitialTopUpLimit(currentPostCount: Int): Int? {
        if (currentPostCount >= GRID_MIN_POST_COUNT) return null
        val missingCount = GRID_MIN_POST_COUNT - currentPostCount
        return ((missingCount + GRID_LOAD_MORE_STEP - 1) / GRID_LOAD_MORE_STEP) * GRID_LOAD_MORE_STEP
    }

    suspend operator fun invoke(
        limit: Int = FEED_PAGE_LIMIT,
        cursor: String? = null,
        userId: String? = null,
    ): ApiResult<PostsFeedData> {
        return postRepository.getNewsfeed(limit = limit, cursor = cursor, userId = userId)
    }
}
