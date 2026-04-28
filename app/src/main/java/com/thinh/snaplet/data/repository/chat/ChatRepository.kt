package com.thinh.snaplet.data.repository.chat

import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.local.entity.ConversationLastMessageStatusProjection
import androidx.paging.PagingData
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.ConversationUpdatedEvent
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageReadEvent
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

    suspend fun getConversations(
        limit: Int = 10,
        cursor: String? = null,
    ): ApiResult<PaginatedResponse<Conversation>>

    suspend fun getMessages(
        conversationId: String,
        limit: Int = 10,
        cursor: String? = null,
    ): ApiResult<PaginatedResponse<Message>>

    fun observeConversations(): Flow<List<ConversationEntity>>
    fun observeLastMessageStatuses(): Flow<List<ConversationLastMessageStatusProjection>>

    suspend fun syncConversations(): ApiResult<Unit>

    suspend fun syncConversationById(convId: String): ApiResult<Unit>

    suspend fun deleteConversationLocal(convId: String)

    suspend fun updateLastMessageLocal(
        convId: String,
        lastMessageAt: Long,
        lastMessageSenderId: String,
        lastMessageText: String? = null,
        lastMessageType: String? = null,
    )

    fun markSeen(conversationId: String, messageId: String, messageCreatedAtMs: Long)

    fun sendTypingStart(conversationId: String)

    fun sendTypingStop(conversationId: String)

    // ── Offline-first message layer ───────────────────────────────────────────

    fun getMessagesPager(convId: String): Flow<PagingData<MessageEntity>>

    suspend fun syncMessages(convId: String, cursor: String? = null): ApiResult<String?>

    suspend fun onIncomingMessage(message: Message)

    suspend fun sendFirstMessage(
        recipientId: String,
        senderId: String,
        text: String,
        mediaKey: String? = null,
        mimeType: String? = null,
        width: Int = 0,
        height: Int = 0,
    ): ApiResult<Message>

    suspend fun sendTextMessage(convId: String, senderId: String, text: String)

    suspend fun sendMediaMessage(convId: String, senderId: String, localUri: String, mediaType: String)

    suspend fun retryMessage(localId: String)

    suspend fun retryPendingMessages(convId: String)

    suspend fun syncOnReconnect(convId: String)
}
