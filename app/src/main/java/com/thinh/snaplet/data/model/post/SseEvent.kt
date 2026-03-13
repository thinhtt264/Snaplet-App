package com.thinh.snaplet.data.model.post

import okhttp3.Response
import okhttp3.sse.EventSource

sealed class SseEvent {
    data class Message(
        val type: String,
        val data: String,
        val id: String?,
    ) : SseEvent()

    data class Opened(val eventSource: EventSource, val response: Response) : SseEvent()

    data class Error(val throwable: Throwable) : SseEvent()

    data object Closed : SseEvent()
    object MaxRetriesExceeded : SseEvent()
}

enum class SseEventType(val value: String) {
    POSTS_UPDATE("posts_update"),
    POSTS_RECONNECT("posts_reconnect"),
    PING("ping"),
    UNKNOWN("unknown");

    companion object {
        fun fromValue(value: String): SseEventType =
            entries.find { it.value == value } ?: UNKNOWN
    }
}