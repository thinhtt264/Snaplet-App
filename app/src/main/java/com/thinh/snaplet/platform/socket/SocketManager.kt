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

private const val LOG_TAG = "SocketManager"

@Singleton
class SocketManager @Inject constructor(
    private val tokenRefreshCoordinator: TokenRefreshCoordinator,
    private val authRepository: Lazy<AuthRepository>,
    private val socketConfig: SocketConfig
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<SocketMessage>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val messages: SharedFlow<SocketMessage> = _messages.asSharedFlow()

    private var socket: Socket? = null

    suspend fun connect() {
        if (socket?.connected() == true) return
        val token = authRepository.get().getAccessToken() ?: return
        connectWithToken(token)
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    private fun connectWithToken(token: String) {
        socket?.off()
        socket?.disconnect()
        socket = null

        _connectionState.value = SocketConnectionState.CONNECTING

        val options = IO.Options().apply {
            auth = mutableMapOf("token" to token)
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
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val message = (args.getOrNull(0) as? JSONObject)?.optString("message")
                ?: args.getOrNull(0)?.toString() ?: ""
            Logger.e("$LOG_TAG: connect_error=$message")

            if (message.contains("Unauthorized", ignoreCase = true)) {
                scope.launch {
                    val newToken = tokenRefreshCoordinator.getNewAccessToken()
                    if (newToken != null) {
                        connectWithToken(newToken)
                    } else {
                        _connectionState.value = SocketConnectionState.ERROR
                    }
                }
            } else {
                _connectionState.value = SocketConnectionState.ERROR
            }
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.getOrNull(0)?.toString()
            Logger.d("$LOG_TAG: disconnected reason=$reason")
            _connectionState.value = SocketConnectionState.DISCONNECTED
        }

        SocketEvent.entries
            .forEach { event ->
                s.on(event.eventName) { args ->
                    Logger.d("$LOG_TAG: event=${event.eventName} args=$args")
                    scope.launch {
                        _messages.emit(SocketMessage(event = event, args = args.toList()))
                    }
                }
            }
    }
}
