package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class RemoveFriendUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        relationshipId: String,
        currentAcceptedFriendCount: Int?,
    ): ApiResult<Unit> {
        if (currentAcceptedFriendCount == null || currentAcceptedFriendCount <= 0) {
            return ApiResult.Failure(
                ApiError(httpCode = 400, message = "No friends to remove")
            )
        }
        return userRepository.removeFriend(relationshipId)
    }
}

