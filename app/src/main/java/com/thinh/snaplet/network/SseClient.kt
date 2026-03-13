package com.thinh.snaplet.network

import com.google.gson.JsonObject
import com.thinh.snaplet.data.model.post.SseEvent
import com.thinh.snaplet.di.StreamOkHttpClient
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.GsonHolder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_CONSECUTIVE_FAILURES = 5
private const val RETRY_DELAY_MS = 2_000L
private const val MAX_RETRY_DELAY_MS = 30_000L

private enum class DisconnectReason { SERVER_CLOSED, FAILURE, UNKNOWN }

@Singleton
class SseClient @Inject constructor(
    @StreamOkHttpClient private val client: OkHttpClient,
) {

    fun connect(url: String, lastEventId: String? = null): Flow<SseEvent> = flow {
        var consecutiveFailures = 0
        var useFreshConnection = false

        while (true) {
            var disconnectReason = DisconnectReason.UNKNOWN

            connectOnce(url, lastEventId, useFreshConnection).collect { event ->
                emit(event)
                when (event) {
                    is SseEvent.Opened -> consecutiveFailures = 0
                    is SseEvent.Closed -> disconnectReason = DisconnectReason.SERVER_CLOSED
                    is SseEvent.Error -> disconnectReason = DisconnectReason.FAILURE
                    else -> Unit
                }
            }

            when (disconnectReason) {
                DisconnectReason.SERVER_CLOSED -> {
                    consecutiveFailures = 0
                    useFreshConnection = true
                    Logger.d("🔄 SSE server-closed, reconnecting with fresh connection in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }

                DisconnectReason.FAILURE -> {
                    consecutiveFailures++
                    useFreshConnection = false
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        Logger.e("❌ SSE max retries ($MAX_CONSECUTIVE_FAILURES) reached, giving up.")
                        emit(SseEvent.MaxRetriesExceeded)
                        return@flow
                    }
                    val delayMs = minOf(
                        RETRY_DELAY_MS * (1L shl (consecutiveFailures - 1)),
                        MAX_RETRY_DELAY_MS
                    )
                    Logger.d("🔄 SSE failure #$consecutiveFailures, retrying in ${delayMs}ms...")
                    delay(delayMs)
                }

                DisconnectReason.UNKNOWN -> {
                    useFreshConnection = false
                    Logger.d("🔄 SSE disconnected unexpectedly, reconnecting in ${RETRY_DELAY_MS}ms...")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
    }

    private fun connectOnce(
        url: String,
        lastEventId: String? = null,
        useFreshConnection: Boolean = false,
    ): Flow<SseEvent> = callbackFlow {
        val httpClient = if (useFreshConnection) {
            client.newBuilder()
                .connectionPool(ConnectionPool(2, 5, TimeUnit.MINUTES))
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Connection", "close")
                            .build()
                    )
                }
                .build()
        } else {
            client
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")

        if (!lastEventId.isNullOrBlank()) {
            requestBuilder.header("Last-Event-ID", lastEventId)
        }

        val request = requestBuilder.build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Logger.d("🔌 onOpen — code=${response.code}, freshConn=$useFreshConnection")
                trySend(SseEvent.Opened(eventSource, response))
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                val resolvedType: String
                val resolvedData: String

                if (type == null) {
                    val json = GsonHolder.gson.fromJson(data, JsonObject::class.java)
                    resolvedType = json.get("type")?.asString.orEmpty()
                    resolvedData = json.get("data")?.asString ?: data
                } else {
                    resolvedType = type
                    resolvedData = data
                }

                trySend(
                    SseEvent.Message(
                        type = resolvedType,
                        data = resolvedData,
                        id = id,
                    ),
                )
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                val error = when {
                    response != null -> IOException("SSE HTTP error: ${response.code}")
                    t != null -> t
                    else -> IOException("Unknown SSE error")
                }
                trySend(SseEvent.Error(error as? Exception ?: IOException(error.message)))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(SseEvent.Closed)
                close()
            }
        }

        val factory = EventSources.createFactory(httpClient)
        val eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}