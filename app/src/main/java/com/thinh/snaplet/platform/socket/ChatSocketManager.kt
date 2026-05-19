package com.thinh.snaplet.platform.socket

import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.network.TokenRefreshCoordinator
import com.thinh.snaplet.utils.Logger
import dagger.Lazy
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "ChatSocketManager"
private val NO_RETRY_DISCONNECT_REASONS = setOf("io client disconnect")

private val CHAT_SOCKET_EVENTS = listOf(
    SocketEvent.CHAT_MESSAGE_NEW,
    SocketEvent.CHAT_MESSAGE_DELETED,
    SocketEvent.CHAT_TYPING_START,
    SocketEvent.CHAT_TYPING_STOP,
    SocketEvent.CHAT_MESSAGE_READ,
    SocketEvent.CHAT_MESSAGE_REACTION_UPDATED,
    SocketEvent.CHAT_CONVERSATION_RESTRICTED,
    SocketEvent.CHAT_CONVERSATION_UNRESTRICTED,
)

@Singleton
class ChatSocketManager @Inject constructor(
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val authRepository: Lazy<AuthRepository>,
    private val socketConfig: SocketConfig,
) : SocketConnector {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<SocketMessage>(
        replay = 0,
        extraBufferCapacity = 64,
    )
    val messages: SharedFlow<SocketMessage> = _messages.asSharedFlow()

    private var socket: Socket? = null

    private val reconnectController = SocketReconnectController(
        connector = this,
        scope = scope,
    )

    private var pendingConversationId: String? = null

    suspend fun connect(conversationId: String) {
        val isSameConversation = pendingConversationId == conversationId
        pendingConversationId = conversationId
        if (socket?.connected() == true && isSameConversation) return
        val token = authRepository.get().getAccessToken() ?: return
        connectWithToken(token, conversationId)
    }

    override suspend fun connect() {
        connect(pendingConversationId ?: return)
    }

    fun disconnect() {
        if (_connectionState.value == SocketConnectionState.DISCONNECTED) return
        pendingConversationId = null
        reconnectController.cancel()
        socket?.disconnect()
        socket = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    private fun connectWithToken(token: String, conversationId: String) {
        socket?.off()
        socket?.disconnect()
        socket = null

        _connectionState.value = SocketConnectionState.CONNECTING

        val options = IO.Options().apply {
            auth = mutableMapOf(
                "token" to token,
                "conversationId" to conversationId,
            )
            forceNew = true
            reconnection = false
        }

        try {
            val s = IO.socket("${socketConfig.baseUrl}/chat", options)
            socket = s
            attachListeners(s)
            s.connect()
        } catch (e: Exception) {
            Logger.e("$LOG_TAG: init failed: ${e.message}")
            _connectionState.value = SocketConnectionState.ERROR
        }
    }

    private fun attachListeners(s: Socket) {
        s.on(Socket.EVENT_CONNECT) {
            Logger.d("$LOG_TAG: connected")
            _connectionState.value = SocketConnectionState.CONNECTED
            reconnectController.onConnectSuccess()
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val message = (args.getOrNull(0) as? JSONObject)?.optString("message")
                ?: args.getOrNull(0)?.toString() ?: ""

            if (message.contains("Unauthorized", ignoreCase = true)) {
                scope.launch {
                    val convId = pendingConversationId ?: return@launch
                    val newToken = tokenRefreshCoordinator.getNewAccessToken()
                    if (newToken != null) {
                        connectWithToken(newToken, convId)
                    } else {
                        reconnectController.onConnectError(errorKey = "Unauthorized")
                        _connectionState.value = SocketConnectionState.ERROR
                    }
                }
            } else {
                reconnectController.onConnectError(errorKey = message.ifBlank { "unknown_error" })
                _connectionState.value = SocketConnectionState.ERROR
            }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.getOrNull(0)?.toString()
            Logger.d("$LOG_TAG: disconnected reason=$reason")
            _connectionState.value = SocketConnectionState.DISCONNECTED

            val normalizedReason = reason?.lowercase()?.trim()
            val shouldSkipRetry = normalizedReason != null &&
                    NO_RETRY_DISCONNECT_REASONS.any {
                        it.equals(
                            normalizedReason,
                            ignoreCase = true
                        )
                    }

            if (!shouldSkipRetry) {
                reconnectController.onConnectError(
                    errorKey = (reason ?: "").ifBlank { "disconnect" }
                )
            }
        }

        CHAT_SOCKET_EVENTS.forEach { event ->
            s.on(event.eventName) { args ->
                val raw = args.getOrNull(0)
                val payload = when (raw) {
                    null -> null
                    is String -> raw
                    is JSONObject -> raw.toString()
                    else -> raw.toString()
                }
                Logger.d("$LOG_TAG: event=${event.eventName} payload=$payload")
                scope.launch {
                    _messages.emit(SocketMessage(event = event, args = payload))
                }
            }
        }
    }

    fun emit(eventName: String, data: JSONObject? = null) {
        val s = socket
        if (s == null || !s.connected()) {
            Logger.w("$LOG_TAG: emit skipped (not connected) event=$eventName")
            return
        }
        if (data != null) s.emit(eventName, data) else s.emit(eventName)
        Logger.d("$LOG_TAG: emit event=$eventName data=$data")
    }
}
