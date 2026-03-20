package com.thinh.snaplet.domain.feed

import com.thinh.snaplet.data.model.post.Post
import java.util.Date
import javax.inject.Inject

/** Oldest unread (non-own) post at or before [viewedPost] in feed order; excludes items newer than [viewedPost] when the list was prepended. */
class ShouldMarkLatestPostAsSeenUseCase @Inject constructor() {

    operator fun invoke(
        posts: List<Post>,
        lastSeenPostCreatedAt: Date?,
        viewedPost: Post,
    ): Post? {
        if (posts.isEmpty()) return null
        val anchorIndex = posts.indexOfFirst { it.id == viewedPost.id }
        if (anchorIndex < 0) return null

        return posts
            .take(anchorIndex + 1)
            .filter { !it.createdAt.after(viewedPost.createdAt) }
            .firstOrNull { post ->
                if (post.isOwnPost) return@firstOrNull false
                lastSeenPostCreatedAt?.let { post.createdAt.after(it) } ?: true
            }
    }
}