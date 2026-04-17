package com.thinh.snaplet.platform.socket

enum class SocketEvent(val eventName: String) {
    POSTS_UNREAD_UPDATED("posts_unread_updated"),
    FRIEND_REQUEST_RECEIVED("friend_request_updated"),
    // ── Chat: received on /chat namespace ──────────────────────────────────
    CHAT_MESSAGE_NEW("chat:message.new"),
    CHAT_MESSAGE_DELETED("chat:message.deleted"),
    CHAT_TYPING_START("chat:typing.start"),
    CHAT_TYPING_STOP("chat:typing.stop"),
    CHAT_MESSAGE_READ("chat:message.read"),
    CHAT_CONVERSATION_UPDATED("chat:conversation.updated"),
    UNKNOWN("unknown");

    companion object {
        fun from(name: String): SocketEvent =
            entries.firstOrNull { it.eventName == name } ?: UNKNOWN
    }
}
