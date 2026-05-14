package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import java.util.Date
import javax.inject.Inject

class MarkMessageSeenUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(conversationId: String, messageId: String, messageCreatedAt: Date) {
        chatRepository.markSeen(conversationId, messageId, messageCreatedAt)
    }
}
