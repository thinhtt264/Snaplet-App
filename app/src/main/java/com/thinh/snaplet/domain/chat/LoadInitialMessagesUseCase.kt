package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class LoadInitialMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(conversationId: String): ApiResult<String?> =
        chatRepository.syncMessages(conversationId)
}
