package com.thinh.snaplet.domain.model

import com.thinh.snaplet.data.model.user.UserSearchResult

data class FriendSearchActionItem(
    val user: UserSearchResult,
    val action: RelationshipAction,
)