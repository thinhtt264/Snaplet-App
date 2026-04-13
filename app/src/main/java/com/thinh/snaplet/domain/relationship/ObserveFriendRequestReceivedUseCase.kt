package com.thinh.snaplet.domain.relationship

import com.thinh.snaplet.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFriendRequestReceivedUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    operator fun invoke(): Flow<Unit> = userRepository.friendRequestReceivedEvents
}

