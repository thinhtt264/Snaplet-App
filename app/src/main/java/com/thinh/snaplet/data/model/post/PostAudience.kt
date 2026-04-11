package com.thinh.snaplet.data.model.post

import com.thinh.snaplet.data.model.RelationshipWithUser

/**
 * Audience when creating a post.
 *
 * [FriendOnly] — API `friend-only` (UI “everyone” / Tất cả). Mutually exclusive with picking specific friends.
 *
 * [SelectedFriends] — API `selected-users` with [friends]’ user ids (non-empty).
 */
sealed class PostAudience {
    data object FriendOnly : PostAudience()

    data class SelectedFriends(val friends: List<RelationshipWithUser>) : PostAudience() {
        init {
            require(friends.isNotEmpty()) { "SelectedFriends requires at least one friend" }
        }
    }
}
