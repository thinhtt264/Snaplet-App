package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.user.UserSearchResult
import com.thinh.snaplet.domain.model.FriendSearchActionItem
import javax.inject.Inject

class FormatFriendSearchResultsUseCase @Inject constructor(
    private val resolveRelationshipActionUseCase: ResolveRelationshipActionUseCase,
) {
    operator fun invoke(
        users: List<UserSearchResult>,
        currentUserId: String?,
    ): List<FriendSearchActionItem> {
        return users.map { user ->
            val statusEnum = RelationshipStatus.from(user.relationshipStatus.orEmpty())
            val action = resolveRelationshipActionUseCase(
                status = statusEnum,
                relationship = null,
                currentUserId = currentUserId,
                targetUserId = user.userId,
            )

            FriendSearchActionItem(
                user = user,
                action = action,
            )
        }
    }
}

