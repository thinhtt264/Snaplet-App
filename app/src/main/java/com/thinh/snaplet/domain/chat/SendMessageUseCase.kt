package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.ApiError
import com.thinh.snaplet.utils.network.ApiResult
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(conversationId: String, text: String): ApiResult<Unit> {
        val senderId = userRepository.getCurrentUserProfile()?.id
            ?: return ApiResult.Failure(ApiError(httpCode = 0, message = "Not logged in"))
        return chatRepository.sendTextMessage(conversationId, senderId, text)
    }
}
