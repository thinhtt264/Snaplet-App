package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class SyncConversationsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    suspend operator fun invoke(): ApiResult<Unit> = chatRepository.syncConversations()
}
