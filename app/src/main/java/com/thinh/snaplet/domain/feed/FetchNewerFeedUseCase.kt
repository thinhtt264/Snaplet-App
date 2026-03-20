package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.post.Post
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase.Companion.FEED_PAGE_LIMIT
import com.thinh.snaplet.domain.model.NewerFeedResult
import com.thinh.snaplet.utils.network.ApiResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class FetchNewerFeedUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {
    companion object {
        const val FEED_MAX_PAGE_INDEX = 1
    }

    /** Fetches posts newer than the newest non-own post in the first [FEED_PAGE_LIMIT] items of [currentPosts]. */
    suspend operator fun invoke(
        unreadCount: Int,
        currentPosts: List<Post>,
    ): NewerFeedResult {
        if (unreadCount < 1) return NewerFeedResult.Empty

        val sinceDate: Date = currentPosts
            .take(FEED_PAGE_LIMIT)
            .filter { !it.isOwnPost }
            .maxByOrNull { it.createdAt }
            ?.createdAt
            ?: return NewerFeedResult.Refresh

        val since = sinceDate.toUtcIsoString()

        return when (
            val result = postRepository.getNewerPost(
                since = since,
                limit = minOf(unreadCount, FEED_PAGE_LIMIT),
            )
        ) {
            is ApiResult.Success -> {
                val newPosts = result.data
                if (newPosts.isEmpty()) return NewerFeedResult.Empty

                val existingIds = currentPosts
                    .filter { !it.isOwnPost }
                    .map { it.id }
                    .toHashSet()

                val toAdd = newPosts.filter { it.id !in existingIds }

                if (toAdd.isEmpty()) return NewerFeedResult.Empty

                val head = currentPosts.take(FEED_PAGE_LIMIT)
                val tail = currentPosts.drop(FEED_PAGE_LIMIT)
                val mergedHead = (head + toAdd)
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt }

                NewerFeedResult.NewPosts(
                    mergedHead = mergedHead,
                    tail = tail,
                )
            }

            is ApiResult.Failure -> NewerFeedResult.Refresh
        }
    }
}

private fun Date.toUtcIsoString(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(this)
}