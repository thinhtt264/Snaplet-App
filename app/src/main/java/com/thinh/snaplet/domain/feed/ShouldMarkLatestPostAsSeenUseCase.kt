package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.Post
import javax.inject.Inject

/**
 * Decides whether the latest post in the feed should be marked as seen.
 *
 * Current business rules:
 * - Do nothing if no item is currently visible (index == -1)
 * - Do nothing if there is no post
 * - Do nothing if the latest post was already marked as seen
 * - Otherwise return the latest post to be marked as seen
 */
class ShouldMarkLatestPostAsSeenUseCase @Inject constructor() {

    operator fun invoke(
        currentVisibleIndex: Int,
        posts: List<Post>,
        lastMarkedPostId: String?
    ): Post? {
        if (currentVisibleIndex < 0) return null
        val firstPost = posts.firstOrNull() ?: return null
        if (firstPost.id == lastMarkedPostId) return null
        return firstPost
    }
}