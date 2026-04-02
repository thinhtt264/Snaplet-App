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
    }

    suspend operator fun invoke(
        limit: Int = FEED_PAGE_LIMIT,
        cursor: String? = null,
        userId: String? = null,
    ): ApiResult<PostsFeedData> {
        return postRepository.getNewsfeed(limit = limit, cursor = cursor, userId = userId)
    }
}
