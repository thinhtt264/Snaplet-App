package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class GetRelationshipsByStatusesUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(statuses: List<RelationshipStatus>): ApiResult<List<RelationshipWithUser>> {
        return userRepository.getRelationshipsByStatuses(statuses)
    }
}

