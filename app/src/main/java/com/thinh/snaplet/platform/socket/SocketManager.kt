package com.thinh.snaplet.platform.socket

import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.network.TokenRefreshCoordinator
import com.thinh.snaplet.utils.Logger
import dagger.Lazy
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "SocketManager"
private val NO_RETRY_DISCONNECT_REASONS = setOf(
    "io client disconnect"
)

interface SocketConnector {
    suspend fun connect()
}

@Singleton
class SocketManager @Inject constructor(
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val authRepository: Lazy<AuthRepository>,
    private val socketConfig: SocketConfig
) : SocketConnector {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sessionId: String = generateSessionId()

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<SocketMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val messages: SharedFlow<SocketMessage> = _messages.asSharedFlow()

    private var socket: Socket? = null

    private val reconnectController = SocketReconnectController(
        connector = this,
        scope = scope
    )

    override suspend fun connect() {
        if (socket?.connected() == true) return
        val token = authRepository.get().getAccessToken() ?: return
        connectWithToken(token)
    }

    fun disconnect() {
        if (_connectionState.value == SocketConnectionState.DISCONNECTED) return
        socket?.disconnect()
//        socket?.off()
        socket = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    private fun connectWithToken(token: String) {
        socket?.off()
        socket?.disconnect()
        socket = null

        _connectionState.value = SocketConnectionState.CONNECTING

        val options = IO.Options().apply {
            auth = mutableMapOf(
                "token" to token,
                "sessionId" to sessionId
            )
            forceNew = true
            reconnection = false
        }

        try {
            val s = IO.socket(socketConfig.baseUrl, options)
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
                    val newToken = tokenRefreshCoordinator.getNewAccessToken()
                    if (newToken != null) {
                        connectWithToken(newToken)
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

        SocketEvent.entries
            .forEach { event ->
                s.on(event.eventName) { args ->
                    val raw = args.getOrNull(0)
                    val payload = when (raw) {
                        is String -> raw
                        is JSONObject -> raw.toString()
                        else -> null
                    }

                    if (payload == null) {
                        Logger.w("$LOG_TAG: ignore event=${event.eventName} because payload is not String/JSONObject. raw=$raw")
                        return@on
                    }

                    Logger.d("$LOG_TAG: event=${event.eventName} payload=$payload")
                    scope.launch {
                        _messages.emit(
                            SocketMessage(
                                event = event,
                                args = payload
                            )
                        )
                    }
                }
            }
    }

    private fun generateSessionId(): String = UUID.randomUUID().toString()

    private class SocketReconnectController(
        private val connector: SocketConnector,
        private val scope: CoroutineScope,
        private val delayMillis: Long = 2_000L,
        private val maxRetriesPerError: Int = 5
    ) {

        private data class ErrorRetryState(
            val errorKey: String,
            val retryCount: Int
        )

        private var currentErrorState: ErrorRetryState? = null
        private var reconnectJob: Job? = null

        fun onConnectSuccess() {
            reconnectJob?.cancel()
            reconnectJob = null
            currentErrorState = null
        }

        fun onConnectError(errorKey: String) {
            val previous = currentErrorState

            val nextState =
                if (previous == null || previous.errorKey != errorKey) {
                    ErrorRetryState(errorKey = errorKey, retryCount = 0)
                } else {
                    previous.copy(retryCount = previous.retryCount + 1)
                }

            currentErrorState = nextState

            if (nextState.retryCount >= maxRetriesPerError) {
                return
            }

            reconnectJob?.cancel()
            reconnectJob = scope.launch {
                delay(delayMillis)
                try {
                    connector.connect()
                } catch (_: Exception) {
                }
            }
        }
    }
}
