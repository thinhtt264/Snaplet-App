package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageType
import com.thinh.snaplet.data.model.chat.SendMessageRequest
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    sealed class Result {
        /** Emitted immediately before the API call — use to show the message optimistically. */
        data class Optimistic(val message: Message) : Result()

        /** Emitted after the API responds successfully — replace the optimistic entry. */
        data class Success(val message: Message) : Result()

        /** Emitted if the API call fails — remove the optimistic entry. */
        data class Failure(val clientUuid: String, val error: String) : Result()
    }

    operator fun invoke(
        conversationId: String,
        senderId: String,
        text: String,
    ): Flow<Result> = flow {
        val clientUuid = UUID.randomUUID().toString()
        val optimistic = Message(
            id = clientUuid, // temp id — replaced on success
            conversationId = conversationId,
            senderId = senderId,
            clientUuid = clientUuid,
            type = MessageType.TEXT,
            content = text,
            isDeleted = false,
            replyTo = null,
            attachments = emptyList(),
            pinnedAt = null,
            createdAt = Date(),
        )
        emit(Result.Optimistic(optimistic))

        chatRepository.sendMessage(
            conversationId = conversationId,
            request = SendMessageRequest(
                clientUuid = clientUuid,
                type = MessageType.TEXT,
                content = text,
            ),
        )
            .onSuccess { emit(Result.Success(it)) }
            .onFailure { emit(Result.Failure(clientUuid, it.message ?: "Unknown error")) }
    }
}
