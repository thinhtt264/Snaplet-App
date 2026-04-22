package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.Logger
import javax.inject.Inject

class MarkMessageReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(conversationId: String, messageId: String) {
        Logger.d("MarkMessageReadUseCase", "mark read: conversationId=$conversationId messageId=$messageId")
        chatRepository.markRead(conversationId, messageId)
    }
}
