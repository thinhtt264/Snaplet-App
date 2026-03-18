package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.Post
import java.util.Date
import javax.inject.Inject

/**
 * Decides whether the latest post in the feed should be marked as seen.
 *
 * Current business rules:
 * - Do nothing if no item is currently visible (index == -1)
 * - Do nothing if there is no post
 * - Do nothing if the latest post was already marked as seen
 * - Ignore optimistic temp posts and own posts
 * - Within the visible range [0, currentVisibleIndex], return the newest eligible post (smallest index)
 */
class ShouldMarkLatestPostAsSeenUseCase @Inject constructor() {

    operator fun invoke(
        currentVisibleIndex: Int,
        posts: List<Post>,
        lastSeenPostCreatedAt: Date?
    ): Post? {
        if (currentVisibleIndex < 0) return null
        if (posts.isEmpty()) return null

        val lastVisibleIndex = currentVisibleIndex.coerceAtMost(posts.lastIndex)

        return posts.take(lastVisibleIndex + 1)
            .firstOrNull { post ->
                // Skip own posts – only friends' posts count for unread
                if (post.isOwnPost) return@firstOrNull false

                lastSeenPostCreatedAt?.let { post.createdAt.after(it) } ?: true
            }
    }
}