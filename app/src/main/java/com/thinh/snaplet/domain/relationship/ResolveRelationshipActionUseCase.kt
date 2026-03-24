package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.domain.model.RelationshipAction
import javax.inject.Inject

class ResolveRelationshipActionUseCase @Inject constructor() {

    operator fun invoke(
        status: RelationshipStatus?,
        relationship: Relationship?,
        currentUserId: String?,
        targetUserId: String,
    ): RelationshipAction {
        if (currentUserId == targetUserId) return RelationshipAction.CurrentUser

        return when (status) {
            RelationshipStatus.ACCEPTED -> RelationshipAction.Accepted
            RelationshipStatus.BLOCKED -> RelationshipAction.Blocked
            RelationshipStatus.PENDING -> if (relationship?.initiator == currentUserId) {
                RelationshipAction.PendingByMe
            } else {
                RelationshipAction.PendingByOther(relationship?.id.orEmpty())
            }
            null -> RelationshipAction.AddFriend
        }
    }
}