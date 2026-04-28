package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import javax.inject.Inject

class MarkMessageSeenUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(conversationId: String, messageId: String, messageCreatedAtMs: Long) {
        chatRepository.markSeen(conversationId, messageId, messageCreatedAtMs)
    }
}
