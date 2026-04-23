package com.thinh.snaplet.data.repository.chat

import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.ConversationUpdatedEvent
import com.thinh.snaplet.data.model.chat.CreateConversationData
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageReadEvent
import com.thinh.snaplet.data.model.chat.SendMessageRequest
import com.thinh.snaplet.platform.socket.SocketConnectionState
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class ChatTypingEvent(
    val userId: String,
    val isTyping: Boolean,
)

interface ChatRepository {

    val newMessages: Flow<Message>

    val typingEvents: Flow<ChatTypingEvent>

    val readReceipts: Flow<MessageReadEvent>

    val conversationUpdates: Flow<ConversationUpdatedEvent>

    val chatSocketConnectionState: StateFlow<SocketConnectionState>

    fun observeUnreadCount(myUserId: String): Flow<Int>

    suspend fun connectChatSocket(conversationId: String)

    fun disconnectChatSocket()

    suspend fun createOrFindConversation(recipientId: String): ApiResult<CreateConversationData>

    suspend fun getConversations(
        limit: Int = 10,
        cursor: String? = null,
    ): ApiResult<PaginatedResponse<Conversation>>

    suspend fun getMessages(
        conversationId: String,
        limit: Int = 10,
        cursor: String? = null,
    ): ApiResult<PaginatedResponse<Message>>

    suspend fun sendMessage(
        conversationId: String,
        request: SendMessageRequest,
    ): ApiResult<Message>

    fun observeConversations(): Flow<List<ConversationEntity>>

    suspend fun syncConversations(): ApiResult<Unit>

    suspend fun syncConversationById(convId: String): ApiResult<Unit>

    suspend fun deleteConversationLocal(convId: String)

    suspend fun updateLastMessageLocal(
        convId: String,
        lastMessageAt: Long,
        lastMessageSenderId: String,
    )

    fun markSeen(conversationId: String, messageId: String)

    fun sendTypingStart(conversationId: String)

    fun sendTypingStop(conversationId: String)
}
