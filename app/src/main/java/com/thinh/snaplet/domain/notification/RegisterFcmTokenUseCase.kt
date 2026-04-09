package com.thinh.snaplet.domain.notification

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class RegisterFcmTokenUseCase @Inject constructor(
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(token: String): ApiResult<Unit> {
        return userRepository.updateFcmToken(token)
    }
}
