package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.model.Relationship
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
            val parsedStatus = user.relationshipStatus?.trim()?.takeIf { it.isNotEmpty() }?.let {
                RelationshipStatus.from(it)
            }
            val statusEnum = parsedStatus
                ?: if (!user.relationshipId.isNullOrBlank()) RelationshipStatus.PENDING else null

            val relationship = if (statusEnum != null && !user.relationshipId.isNullOrBlank()) {
                Relationship(
                    id = user.relationshipId,
                    user1Id = "",
                    user2Id = "",
                    status = user.relationshipStatus?.trim().takeUnless { it.isNullOrEmpty() }
                        ?: statusEnum.value,
                    initiator = user.initiator.orEmpty(),
                    createdAt = user.relationshipCreatedAt.orEmpty(),
                    updatedAt = "",
                )
            } else {
                null
            }

            val action = resolveRelationshipActionUseCase(
                status = statusEnum,
                relationship = relationship,
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

