package com.thinh.snaplet.ui.screens.friend_request

import com.thinh.snaplet.data.model.user.UserProfile
import com.thinh.snaplet.domain.model.RelationshipAction

data class FriendRequestUiState(
    val userProfile: UserProfile? = null,
    val relationshipAction: RelationshipAction? = null,
    val isRequesting: Boolean = false,
    val isLoading: Boolean = true,
) {
    val isCurrentUser: Boolean get() = relationshipAction == RelationshipAction.CurrentUser
}
