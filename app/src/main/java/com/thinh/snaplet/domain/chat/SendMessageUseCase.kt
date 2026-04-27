package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(convId: String, senderId: String, text: String) {
        chatRepository.sendTextMessage(convId, senderId, text)
    }
}
