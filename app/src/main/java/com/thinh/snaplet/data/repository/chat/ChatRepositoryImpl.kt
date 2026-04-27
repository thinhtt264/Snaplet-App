package com.thinh.snaplet.data.repository.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.local.dao.ConversationDao
import com.thinh.snaplet.data.local.dao.MessageDao
import com.thinh.snaplet.data.local.dao.MessageRemoteKeyDao
import com.thinh.snaplet.data.local.db.AppDatabase
import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.local.entity.MessageRemoteKeyEntity
import com.thinh.snaplet.data.local.entity.MessageStatus
import com.thinh.snaplet.data.model.PaginatedResponse
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.ConversationUpdatedEvent
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageReadEvent
import com.thinh.snaplet.data.model.chat.MessageType
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
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val CHAT_TYPING_START = "chat:typing_start"
private const val CHAT_TYPING_STOP = "chat:typing_stop"
private const val PAGE_SIZE = 20

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val messageRemoteKeyDao: MessageRemoteKeyDao,
    private val appDatabase: AppDatabase,
    private val chatSocketManager: ChatSocketManager,
    socketManager: SocketManager,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val newMessages: Flow<Message> =
        chatSocketManager.messages.filter { it.event == SocketEvent.CHAT_MESSAGE_NEW }
            .mapNotNull { message ->
                val json = message.args ?: return@mapNotNull null
                runCatching {
                    gson.fromJson(json, Message::class.java)
                }.onFailure {
                    Logger.e("parse chat:message.new failed: ${it.message}")
                }.getOrNull()
            }

    override val typingEvents: Flow<ChatTypingEvent> = chatSocketManager.messages.filter {
        it.event == SocketEvent.CHAT_TYPING_START || it.event == SocketEvent.CHAT_TYPING_STOP
    }.mapNotNull { message ->
        val json = message.args ?: return@mapNotNull null
        runCatching {
            val payload = gson.fromJson(
                json, com.thinh.snaplet.data.model.chat.TypingPayload::class.java
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
        chatSocketManager.messages.filter { it.event == SocketEvent.CHAT_MESSAGE_READ }
            .mapNotNull { message ->
                val json = message.args ?: return@mapNotNull null
                runCatching {
                    gson.fromJson(json, MessageReadEvent::class.java)
                }.onFailure {
                    Logger.e("parse chat:message.read failed: ${it.message}")
                }.getOrNull()
            }

    override val conversationUpdates: Flow<ConversationUpdatedEvent> =
        socketManager.messages.filter { it.event == SocketEvent.CHAT_CONVERSATION_UPDATED }
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
        routeIncomingMessagesToRoom()
    }

    private fun observeConversationUpdatedSocket(socketManager: SocketManager) {
        scope.launch {
            socketManager.messages.filter { it.event == SocketEvent.CHAT_CONVERSATION_UPDATED }
                .mapNotNull { message ->
                    val json = message.args ?: return@mapNotNull null
                    runCatching {
                        gson.fromJson(json, ConversationUpdatedEvent::class.java)
                    }.getOrNull()
                }.collect { event ->
                    val existing = conversationDao.getById(event.conversationId)
                    if (existing == null) {
                        syncConversationById(event.conversationId)
                    } else {
                        updateLastMessageLocal(
                            convId = event.conversationId,
                            lastMessageAt = event.lastMessageAt.time,
                            lastMessageSenderId = event.lastMessageSenderId,
                            lastMessageText = event.lastMessageText
                        )
                    }
                }
        }
    }

    private fun observeConversationDeletedSocket(socketManager: SocketManager) {
        scope.launch {
            socketManager.messages.filter { it.event == SocketEvent.CHAT_CONVERSATION_DELETED }
                .mapNotNull { message ->
                    val json = message.args ?: return@mapNotNull null
                    runCatching {
                        JSONObject(json).optString("conversationId").takeIf { it.isNotEmpty() }
                    }.getOrNull()
                }.collect { convId ->
                    Logger.d("conversation_deleted convId=$convId")
                    deleteConversationLocal(convId)
                }
        }
    }

    private fun routeIncomingMessagesToRoom() {
        scope.launch {
            newMessages.collect { message -> onIncomingMessage(message) }
        }
    }

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

    override suspend fun getConversations(
        limit: Int,
        cursor: String?,
    ): ApiResult<PaginatedResponse<Conversation>> {
        return safeApiCall(
            apiCall = { apiService.getConversations(limit = limit, cursor = cursor) },
            onSuccess = { data ->
                val incoming = data.data
                if (incoming.isEmpty() && cursor == null) {
                    conversationDao.deleteAll()
                } else {
                    upsertIfChanged(incoming)
                }
            },
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
            })
    }

    override fun markSeen(conversationId: String, messageId: String) {
        Logger.d("markSeen conversationId=$conversationId messageId=$messageId")
        val now = System.currentTimeMillis()
        scope.launch(NonCancellable) {
            conversationDao.updateMyLastSeenAt(conversationId, now)
            safeApiCall(apiCall = { apiService.markMessageSeen(conversationId, messageId) })
        }
    }

    override fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAll()

    override suspend fun syncConversations(): ApiResult<Unit> {
        return safeApiCall(
            apiCall = { apiService.getConversations(limit = 20, cursor = null) },
            onSuccess = { data ->
                val incoming = data.data
                if (incoming.isEmpty()) {
                    conversationDao.deleteAll()
                } else {
                    upsertIfChanged(incoming)
                }
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
        lastMessageText: String?,
        lastMessageType: String?,
    ) {
        conversationDao.updateLastMessage(
            convId,
            lastMessageAt,
            lastMessageSenderId,
            lastMessageText,
            lastMessageType
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

    // ── Offline-first message layer ───────────────────────────────────────────

    @OptIn(ExperimentalPagingApi::class)
    override fun getMessagesPager(convId: String): Flow<PagingData<MessageEntity>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = MessageRemoteMediator(
            convId = convId,
            apiService = apiService,
            db = appDatabase,
        ),
        pagingSourceFactory = { appDatabase.messageDao().pagingSource(convId) },
    ).flow

    override suspend fun syncMessages(convId: String, cursor: String?): ApiResult<String?> {
        Logger.d("syncMessages convId=$convId cursor=$cursor")
        return safeApiCall(
            apiCall = { apiService.getMessages(convId, limit = PAGE_SIZE, cursor = cursor) },
            onSuccess = { data ->
                appDatabase.withTransaction {
                    messageDao.upsertAll(data.data.map { it.toEntity() })
                    messageRemoteKeyDao.upsert(
                        MessageRemoteKeyEntity(
                            conversationId = convId,
                            nextCursor = data.pagination.nextCursor,
                        )
                    )
                }
            },
            transform = { data -> data.pagination.nextCursor },
        )
    }

    override suspend fun onIncomingMessage(message: Message) {
        // If we have an optimistic PENDING row for this clientUuid, delete it before
        // upserting the server-confirmed entity to avoid a PRIMARY KEY conflict.
        messageDao.deletePendingByLocalId(message.clientUuid)
        messageDao.upsert(message.toEntity())
        conversationDao.updateLastMessage(
            convId = message.conversationId,
            lastMessageAt = message.createdAt.time,
            lastMessageSenderId = message.senderId,
            lastMessageText = message.text,
            lastMessageType = message.messageType.name.lowercase(),
        )
    }

    override suspend fun sendFirstMessage(recipientId: String, senderId: String, text: String): ApiResult<Message> {
        val localId = UUID.randomUUID().toString()
        Logger.d("sendFirstMessage recipientId=$recipientId clientUuid=$localId")
        return safeApiCall(
            apiCall = {
                apiService.sendMessage(
                    body = SendMessageRequest(
                        recipientId = recipientId,
                        clientUuid = localId,
                        text = text,
                    )
                )
            },
            onSuccess = { message ->
                if (conversationDao.getById(message.conversationId) == null) {
                    syncConversationById(message.conversationId)
                } else {
                    conversationDao.updateLastMessage(
                        convId = message.conversationId,
                        lastMessageAt = message.createdAt.time,
                        lastMessageSenderId = message.senderId,
                        lastMessageText = message.text,
                        lastMessageType = message.messageType.name.lowercase(),
                    )
                }
                messageDao.upsert(message.toEntity())
            },
        )
    }

    override suspend fun sendTextMessage(convId: String, senderId: String, text: String) {
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        messageDao.insert(
            MessageEntity(
                id = localId,
                localId = localId,
                conversationId = convId,
                senderId = senderId,
                type = MessageType.TEXT.name.lowercase(),
                text = text,
                mediaUrl = null,
                mediaLocalUri = null,
                mediaType = null,
                status = MessageStatus.PENDING,
                isDeleted = false,
                createdAt = now,
                serverCreatedAt = null,
            )
        )
        conversationDao.updateLastMessage(
            convId,
            now,
            senderId,
            text,
            MessageType.TEXT.name.lowercase()
        )
        scope.launch(NonCancellable) { executeSend(localId) }
    }

    override suspend fun sendMediaMessage(
        convId: String, senderId: String, localUri: String, mediaType: String
    ) {
        // TODO: implement 3-step media upload flow once MediaRepository signatures are confirmed
        Logger.w("sendMediaMessage not yet implemented localUri=$localUri")
    }

    override suspend fun retryMessage(localId: String) {
        val msg = messageDao.getByLocalId(localId) ?: return
        messageDao.updateStatus(localId, MessageStatus.PENDING)
        when {
            msg.type == MessageType.TEXT.name.lowercase() -> executeSend(localId)
            msg.mediaLocalUri != null -> Logger.w("retryMessage: media retry not yet implemented")
            msg.mediaUrl != null -> executeSend(localId)
        }
    }

    override suspend fun retryPendingMessages(convId: String) {
        messageDao.getPendingByConvId(convId).forEach { msg ->
            when {
                msg.type == MessageType.TEXT.name.lowercase() -> executeSend(msg.localId)
                msg.mediaUrl != null -> executeSend(msg.localId)
                msg.mediaLocalUri != null -> Logger.w("retryPending: media retry not yet implemented localId=${msg.localId}")
            }
        }
    }

    override suspend fun syncOnReconnect(convId: String) {
        syncMessages(convId)
        retryPendingMessages(convId)
    }

    private suspend fun executeSend(localId: String) {
        val msg = messageDao.getByLocalId(localId) ?: return
        val conv = conversationDao.getById(msg.conversationId) ?: run {
            Logger.e("executeSend: conversation not found for convId=${msg.conversationId}")
            messageDao.updateStatus(localId, MessageStatus.FAILED)
            return
        }
        safeApiCall(
            apiCall = {
                apiService.sendMessage(
                    body = SendMessageRequest(
                        recipientId = conv.participantId,
                        clientUuid = localId,
                        text = msg.text,
                    ),
                )
            }).onSuccess { sent ->
            messageDao.updateStatusAfterSend(
                localId = localId,
                serverId = sent.id,
                status = MessageStatus.SENT,
                serverCreatedAt = sent.createdAt.time,
            )
        }.onFailure {
            Logger.e("executeSend failed localId=$localId: ${it.message}")
            messageDao.updateStatus(localId, MessageStatus.FAILED)
        }
    }

    private suspend fun upsertIfChanged(incoming: List<Conversation>) {
        if (incoming.isEmpty()) return
        val snapshot = conversationDao.getAllUpdatedAtSnapshot().associateBy { it.id }
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
