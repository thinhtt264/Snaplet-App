package com.thinh.snaplet.network

import com.thinh.snaplet.data.model.post.SseEvent
import com.thinh.snaplet.di.InternalOkHttpClient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

@Singleton
class SseClient @Inject constructor(
    @InternalOkHttpClient okHttpClient: OkHttpClient,
) {

    private val client: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    fun connect(url: String, lastEventId: String? = null): Flow<SseEvent> = callbackFlow {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")

        // TODO: send Last-Event-ID header when BE supports replay
        if (!lastEventId.isNullOrBlank()) {
            requestBuilder.header("Last-Event-ID", lastEventId)
        }

        val request = requestBuilder.build()

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                trySend(
                    SseEvent.Message(
                        type = type.orEmpty(),
                        data = data,
                        id = id,
                    ),
                )
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                val error = t ?: IOException("Unknown SSE error")
                trySend(SseEvent.Error(error))
            }

            override fun onClosed(eventSource: EventSource) {
                trySend(SseEvent.Closed)
                close()
            }
        }

        val factory = EventSources.createFactory(client)
        val eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}