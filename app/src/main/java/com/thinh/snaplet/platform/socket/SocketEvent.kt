package com.thinh.snaplet.platform.socket

enum class SocketEvent(val eventName: String) {
    NEW_POST("new_post"),
    // NEW_CHAT("new_chat"),
    UNKNOWN("unknown");

    companion object {
        fun from(name: String): SocketEvent =
            entries.firstOrNull { it.eventName == name } ?: UNKNOWN
    }
}
