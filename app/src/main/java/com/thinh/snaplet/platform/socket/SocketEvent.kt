package com.thinh.snaplet.platform.socket

enum class SocketEvent(val eventName: String) {
    POSTS_UNREAD_UPDATED("posts_unread_updated"),
    // NEW_CHAT("new_chat"),
    UNKNOWN("unknown");

    companion object {
        fun from(name: String): SocketEvent =
            entries.firstOrNull { it.eventName == name } ?: UNKNOWN
    }
}
