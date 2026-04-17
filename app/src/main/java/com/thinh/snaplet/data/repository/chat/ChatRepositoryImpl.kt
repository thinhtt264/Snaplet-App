package com.thinh.snaplet.data.repository.chat

import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.ConversationUpdatedEvent
import com.thinh.snaplet.data.model.chat.CreateConversationData
import com.thinh.snaplet.data.model.chat.CreateConversationRequest
import com.thinh.snaplet.data.model.chat.MarkReadPayload
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageReadEvent
import com.thinh.snaplet.data.model.chat.SendMessageRequest
import com.thinh.snaplet.data.model.chat.TypingPayload
import com.thinh.snaplet.data.model.chat.TypingSocketPayload
import com.thinh.snaplet.platform.socket.ChatSocketManager
import com.thinh.snaplet.platform.socket.SocketConnectionState
import com.thinh.snaplet.platform.socket.SocketEvent
import com.thinh.snaplet.platform.socket.SocketManager
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.GsonHolder.gson
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val CHAT_MARK_READ = "chat:mark_read"
private const val CHAT_TYPING_START = "chat:typing_start"
private const val CHAT_TYPING_STOP = "chat:typing_stop"

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val chatSocketManager: ChatSocketManager,
    socketManager: SocketManager,
) : ChatRepository {

    override val newMessages: Flow<Message> =
        chatSocketManager.messages
            .filter { it.event == SocketEvent.CHAT_MESSAGE_NEW }
            .mapNotNull { message ->
                val json = message.args ?: return@mapNotNull null
                runCatching {
                    gson.fromJson(json, Message::class.java)
                }.onFailure {
                    Logger.e("parse chat:message.new failed: ${it.message}")
                }.getOrNull()
            }

    override val typingEvents: Flow<ChatTypingEvent> =
        chatSocketManager.messages
            .filter {
                it.event == SocketEvent.CHAT_TYPING_START ||
                        it.event == SocketEvent.CHAT_TYPING_STOP
            }
            .mapNotNull { message ->
                val json = message.args ?: return@mapNotNull null
                runCatching {
                    val payload = gson.fromJson(json, TypingPayload::class.java)
                    ChatTypingEvent(
                        userId = payload.userId,
                        isTyping = message.event == SocketEvent.CHAT_TYPING_START,
                    )
                }.onFailure {
                    Logger.e("parse typing event failed: ${it.message}")
                }.getOrNull()
            }

    override val readReceipts: Flow<MessageReadEvent> =
        chatSocketManager.messages
            .filter { it.event == SocketEvent.CHAT_MESSAGE_READ }
            .mapNotNull { message ->
                val json = message.args ?: return@mapNotNull null
                runCatching {
                    gson.fromJson(json, MessageReadEvent::class.java)
                }.onFailure {
                    Logger.e("parse chat:message.read failed: ${it.message}")
                }.getOrNull()
            }

    // ── Global socket: incoming flows ─────────────────────────────────────

    override val conversationUpdates: Flow<ConversationUpdatedEvent> =
        socketManager.messages
            .filter { it.event == SocketEvent.CHAT_CONVERSATION_UPDATED }
            .mapNotNull { message ->
                val json = message.args ?: return@mapNotNull null
                runCatching {
                    gson.fromJson(json, ConversationUpdatedEvent::class.java)
                }.onFailure {
                    Logger.e("parse chat:conversation.updated failed: ${it.message}")
                }.getOrNull()
            }

    // ── /chat socket lifecycle ────────────────────────────────────────────

    override val chatSocketConnectionState: StateFlow<SocketConnectionState> =
        chatSocketManager.connectionState

    override suspend fun connectChatSocket(conversationId: String) {
        Logger.d("connectChatSocket conversationId=$conversationId")
        chatSocketManager.connect(conversationId)
    }

    override fun disconnectChatSocket() {
        Logger.d("disconnectChatSocket")
        chatSocketManager.disconnect()
    }

    // ── REST API ──────────────────────────────────────────────────────────

    override suspend fun createOrFindConversation(recipientId: String): ApiResult<CreateConversationData> {
        Logger.d("createOrFindConversation recipientId=$recipientId")
        return safeApiCall(
            apiCall = { apiService.createOrFindConversation(CreateConversationRequest(recipientId)) }
        )
    }

    override suspend fun getConversations(
        limit: Int,
        cursor: String?,
    ): ApiResult<PaginatedResponse<Conversation>> {
        Logger.d("getConversations limit=$limit cursor=$cursor")
        return safeApiCall(
            apiCall = { apiService.getConversations(limit = limit, cursor = cursor) }
        )
    }

    override suspend fun getMessages(
        conversationId: String,
        limit: Int,
        cursor: String?,
    ): ApiResult<PaginatedResponse<Message>> {
        Logger.d("getMessages conversationId=$conversationId limit=$limit cursor=$cursor")
        return safeApiCall(
            apiCall = {
                apiService.getMessages(
                    conversationId = conversationId,
                    limit = limit,
                    cursor = cursor,
                )
            }
        )
    }

    override suspend fun sendMessage(
        conversationId: String,
        request: SendMessageRequest,
    ): ApiResult<Message> {
        Logger.d("sendMessage conversationId=$conversationId clientUuid=${request.clientUuid}")
        return safeApiCall(
            apiCall = { apiService.sendMessage(conversationId = conversationId, body = request) }
        )
    }

    // ── WebSocket outgoing events ─────────────────────────────────────────

    override fun markRead(conversationId: String, messageId: String) {
        Logger.d("markRead conversationId=$conversationId messageId=$messageId")
        chatSocketManager.emit(
            eventName = CHAT_MARK_READ,
            data = JSONObject(gson.toJson(MarkReadPayload(conversationId, messageId))),
        )
    }

    override fun sendTypingStart(conversationId: String) {
        Logger.d("sendTypingStart conversationId=$conversationId")
        chatSocketManager.emit(
            eventName = CHAT_TYPING_START,
            data = JSONObject(gson.toJson(TypingSocketPayload(conversationId))),
        )
    }

    override fun sendTypingStop(conversationId: String) {
        Logger.d("sendTypingStop conversationId=$conversationId")
        chatSocketManager.emit(
            eventName = CHAT_TYPING_STOP,
            data = JSONObject(gson.toJson(TypingSocketPayload(conversationId))),
        )
    }
}
