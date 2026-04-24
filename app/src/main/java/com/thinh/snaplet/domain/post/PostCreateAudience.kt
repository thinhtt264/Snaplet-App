package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.post.PostVisibility

/**
 * Audience for creating a post — maps to backend `PostVisibility` and optional `allowedViewerUserIds`.
 */
sealed class PostCreateAudience {
    data object FriendOnly : PostCreateAudience()

    data class SelectedUsers(val userIds: List<String>) : PostCreateAudience()

    val apiVisibility: String
        get() = when (this) {
            is FriendOnly -> PostVisibility.FRIEND_ONLY
            is SelectedUsers -> PostVisibility.SELECTED_USERS
        }
}
