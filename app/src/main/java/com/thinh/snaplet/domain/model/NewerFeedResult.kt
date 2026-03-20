package com.thinh.snaplet.domain.model

import com.thinh.snaplet.data.model.post.Post

sealed class NewerFeedResult {
    data class NewPosts(
        val mergedHead: List<Post>,
        val tail: List<Post>,
    ) : NewerFeedResult()

    data object Refresh : NewerFeedResult()
    data object Empty : NewerFeedResult()
}