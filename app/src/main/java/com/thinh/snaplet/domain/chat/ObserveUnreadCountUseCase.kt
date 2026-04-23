package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUnreadCountUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    operator fun invoke(userId: String): Flow<Int> =
        chatRepository.observeUnreadCount(myUserId = userId)
}
