package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.model.Relationship
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class AcceptFriendRequestUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(relationshipId: String): ApiResult<Relationship> {
        return userRepository.acceptFriendRequest(relationshipId)
    }
}

