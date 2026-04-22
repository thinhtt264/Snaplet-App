package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.Logger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNewMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    operator fun invoke(): Flow<Message> {
        Logger.d("ObserveNewMessageUseCase", "subscribed")
        return chatRepository.newMessages
    }
}
