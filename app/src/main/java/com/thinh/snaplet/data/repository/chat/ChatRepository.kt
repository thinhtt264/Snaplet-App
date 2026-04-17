package com.thinh.snaplet.data.repository.chat

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

    suspend fun connectChatSocket(conversationId: String)

    fun disconnectChatSocket()

    /** POST /api/v1/conversations — create or find an existing conversation. */
    suspend fun createOrFindConversation(recipientId: String): ApiResult<CreateConversationData>

    /**
     * GET /api/v1/conversations — list conversations with cursor-based pagination.
     * [nextCursor] == null means no more pages.
     */
    suspend fun getConversations(
        limit: Int = 10,
        cursor: String? = null,
    ): ApiResult<PaginatedResponse<Conversation>>

    /**
     * GET /api/v1/conversations/{conversationId}/messages
     * Cursor-based pagination. [nextCursor] == null means no more pages.
     */
    suspend fun getMessages(
        conversationId: String,
        limit: Int = 10,
        cursor: String? = null,
    ): ApiResult<PaginatedResponse<Message>>

    /** POST /api/v1/conversations/{conversationId}/messages — send a message. */
    suspend fun sendMessage(
        conversationId: String,
        request: SendMessageRequest,
    ): ApiResult<Message>

    // ── WebSocket outgoing events (/chat) ──────────────────────────────────

    /** Emit `chat:mark_read` — call after the user has read up to [messageId]. */
    fun markRead(conversationId: String, messageId: String)

    /** Emit `chat:typing_start` — call when the user starts typing. */
    fun sendTypingStart(conversationId: String)

    /** Emit `chat:typing_stop` — call when the user stops typing. */
    fun sendTypingStop(conversationId: String)
}
