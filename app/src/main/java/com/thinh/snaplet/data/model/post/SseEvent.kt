package com.thinh.snaplet.data.model.post

sealed class SseEvent {
    data class Message(
        val type: String,
        val data: String,
        val id: String?,
    ) : SseEvent()

    data class Error(val throwable: Throwable) : SseEvent()

    data object Closed : SseEvent()
}