package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

/** Removes a relationship by ID (e.g. dismiss pending request). No friends-count validation. */
class RemoveRelationshipUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(relationshipId: String): ApiResult<Unit> {
        return userRepository.removeFriend(relationshipId)
    }
}

