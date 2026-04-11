package com.thinh.snaplet.domain.post

/**
 * Audience for creating a post — maps to backend `PostVisibility` and optional `allowedViewerUserIds`.
 */
sealed class PostCreateAudience {
    data object FriendOnly : PostCreateAudience()

    data class SelectedUsers(val userIds: List<String>) : PostCreateAudience()

    val apiVisibility: String
        get() = when (this) {
            is FriendOnly -> "friend-only"
            is SelectedUsers -> "selected-users"
        }
}
