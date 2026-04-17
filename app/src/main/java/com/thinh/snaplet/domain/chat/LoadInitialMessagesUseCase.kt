package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class LoadInitialMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(
        conversationId: String,
        limit: Int,
    ): ApiResult<PaginatedResponse<Message>> =
        chatRepository.getMessages(conversationId, limit)
}
