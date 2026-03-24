package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.domain.model.RelationshipAction
import javax.inject.Inject

class GetRelationshipActionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val resolveRelationshipActionUseCase: ResolveRelationshipActionUseCase,
) {
    suspend operator fun invoke(targetUserId: String): RelationshipAction {
        val currentUser = userRepository.getCurrentUserProfile()
        val isCurrentUser = currentUser?.id == targetUserId
        if (isCurrentUser) return RelationshipAction.CurrentUser

        val relationship = userRepository.getRelationshipWithUser(targetUserId)
            .fold(onSuccess = { it }, onFailure = { null })

        val status = RelationshipStatus.from(relationship?.status.orEmpty())

        return resolveRelationshipActionUseCase(
            status = status,
            relationship = relationship,
            currentUserId = currentUser?.id,
            targetUserId = targetUserId
        )
    }
}

