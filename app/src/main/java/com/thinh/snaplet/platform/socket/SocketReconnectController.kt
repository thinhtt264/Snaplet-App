package com.thinh.snaplet.platform.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SocketReconnectController(
    private val connector: SocketConnector,
    private val scope: CoroutineScope,
    private val delayMillis: Long = 2_000L,
    private val maxRetriesPerError: Int = 5,
) {
    private data class ErrorRetryState(val errorKey: String, val retryCount: Int)

    private var currentErrorState: ErrorRetryState? = null
    private var reconnectJob: Job? = null

    fun onConnectSuccess() {
        reconnectJob?.cancel()
        reconnectJob = null
        currentErrorState = null
    }

    fun onConnectError(errorKey: String) {
        val previous = currentErrorState

        val nextState = if (previous == null || previous.errorKey != errorKey) {
            ErrorRetryState(errorKey = errorKey, retryCount = 0)
        } else {
            previous.copy(retryCount = previous.retryCount + 1)
        }

        currentErrorState = nextState

        if (nextState.retryCount >= maxRetriesPerError) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMillis)
            try {
                connector.connect()
            } catch (_: Exception) {
            }
        }
    }

    /** Cancel any pending reconnect attempt. Call on intentional disconnect. */
    fun cancel() {
        reconnectJob?.cancel()
        reconnectJob = null
        currentErrorState = null
    }
}
