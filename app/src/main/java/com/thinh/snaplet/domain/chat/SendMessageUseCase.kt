package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(conversationId: String, text: String) {
        val senderId = userRepository.getCurrentUserProfile()?.id
            ?: error("Not logged in")
        chatRepository.sendTextMessage(conversationId, senderId, text).fold(
            onSuccess = { },
            onFailure = { err -> error(err.message) },
        )
    }
}
