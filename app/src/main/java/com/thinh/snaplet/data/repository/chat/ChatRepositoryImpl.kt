package com.thinh.snaplet.data.repository.chat

import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.local.dao.ConversationDao
import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.ConversationUpdatedEvent
import com.thinh.snaplet.data.model.chat.CreateConversationData
import com.thinh.snaplet.data.model.chat.CreateConversationRequest
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageReadEvent
import com.thinh.snaplet.data.model.chat.SendMessageRequest
import com.thinh.snaplet.data.model.chat.TypingSocketPayload
import com.thinh.snaplet.data.model.chat.toEntity
import com.thinh.snaplet.platform.socket.ChatSocketManager
import com.thinh.snaplet.platform.socket.SocketConnectionState
import com.thinh.snaplet.platform.socket.SocketEvent
import com.thinh.snaplet.platform.socket.SocketManager
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.ApiResult
import com.thinh.snaplet.utils.network.GsonHolder.gson
import com.thinh.snaplet.utils.network.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val CHAT_TYPING_START = "chat:typing_start"
private const val CHAT_TYPING_STOP = "chat:typing_stop"

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val conversationDao: ConversationDao,
    private val chatSocketManager: ChatSocketManager,
    socketManager: SocketManager,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                    val payload = gson.fromJson(
                        json,
                        com.thinh.snaplet.data.model.chat.TypingPayload::class.java
                    )
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

    init {
        observeConversationUpdatedSocket(socketManager)
        observeConversationDeletedSocket(socketManager)
    }

    private fun observeConversationUpdatedSocket(socketManager: SocketManager) {
        scope.launch {
            socketManager.messages
                .filter { it.event == SocketEvent.CHAT_CONVERSATION_UPDATED }
                .mapNotNull { message ->
                    val json = message.args ?: return@mapNotNull null
                    runCatching {
                        gson.fromJson(json, ConversationUpdatedEvent::class.java)
                    }.getOrNull()
                }
                .collect { event ->
                    Logger.d("conversation_updated convId=${event.conversationId} → updateLastMessage")
                    updateLastMessageLocal(
                        convId = event.conversationId,
                        lastMessageAt = event.lastMessageAt.time,
                        lastMessageSenderId = event.lastMessageSenderId,
                    )
                }
        }
    }

    private fun observeConversationDeletedSocket(socketManager: SocketManager) {
        scope.launch {
            socketManager.messages
                .filter { it.event == SocketEvent.CHAT_CONVERSATION_DELETED }
                .mapNotNull { message ->
                    val json = message.args ?: return@mapNotNull null
                    runCatching {
                        JSONObject(json).optString("conversationId").takeIf { it.isNotEmpty() }
                    }.getOrNull()
                }
                .collect { convId ->
                    Logger.d("conversation_deleted convId=$convId → delete local")
                    deleteConversationLocal(convId)
                }
        }
    }

    // ── /chat socket lifecycle ────────────────────────────────────────────────

    override val chatSocketConnectionState: StateFlow<SocketConnectionState> =
        chatSocketManager.connectionState

    override fun observeUnreadCount(myUserId: String): Flow<Int> =
        conversationDao.observeUnreadCount(myUserId).distinctUntilChanged()

    override suspend fun connectChatSocket(conversationId: String) {
        Logger.d("connectChatSocket conversationId=$conversationId")
        chatSocketManager.connect(conversationId)
    }

    override fun disconnectChatSocket() {
        Logger.d("disconnectChatSocket")
        chatSocketManager.disconnect()
    }

    // ── REST API ──────────────────────────────────────────────────────────────

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
            apiCall = { apiService.getConversations(limit = limit, cursor = cursor) },
            onSuccess = { data -> upsertIfChanged(data.data) },
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

    override fun markSeen(conversationId: String, messageId: String) {
        Logger.d("Mark seen conversationId=$conversationId messageId=$messageId")
        val now = System.currentTimeMillis()
        scope.launch(NonCancellable) {
            conversationDao.updateMyLastSeenAt(conversationId, now)
            safeApiCall(apiCall = { apiService.markMessageSeen(conversationId, messageId) })
        }
    }

    override fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAll()

    override suspend fun syncConversations(): ApiResult<Unit> {
        Logger.d("syncConversations")
        return safeApiCall(
            apiCall = { apiService.getConversations(limit = 20, cursor = null) },
            onSuccess = { data ->
                upsertIfChanged(data.data)
            },
            transform = {},
        )
    }

    override suspend fun syncConversationById(convId: String): ApiResult<Unit> {
        Logger.d("syncConversationById convId=$convId")
        return safeApiCall(
            apiCall = { apiService.getConversationById(convId) },
            onSuccess = { conv -> conversationDao.upsert(conv.toEntity()) },
            transform = {},
        )
    }

    override suspend fun deleteConversationLocal(convId: String) {
        Logger.d("deleteConversationLocal convId=$convId")
        conversationDao.deleteById(convId)
    }

    override suspend fun updateLastMessageLocal(
        convId: String,
        lastMessageAt: Long,
        lastMessageSenderId: String,
    ) {
        conversationDao.updateLastMessage(convId, lastMessageAt, lastMessageSenderId)
    }

    // ── WebSocket outgoing events ─────────────────────────────────────────────

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

    private suspend fun upsertIfChanged(incoming: List<Conversation>) {
        if (incoming.isEmpty()) return
        val snapshot = conversationDao.getAllUpdatedAtSnapshot()
            .associateBy { it.id }

        val changed = incoming.filter { conv ->
            val existing = snapshot[conv.id]
            existing == null || existing.updatedAt != conv.updatedAt.time
        }
        if (changed.isNotEmpty()) {
            Logger.d("upsertIfChanged: ${changed.size}/${incoming.size} changed")
            conversationDao.upsertAll(changed.map { it.toEntity() })
        }
    }
}
