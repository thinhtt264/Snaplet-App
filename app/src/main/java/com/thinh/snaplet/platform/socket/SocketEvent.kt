package com.thinh.snaplet.platform.socket

/**
 * Chat socket events emitted by client to server.
 */
object ChatSocketEmitEvent {
    const val JOIN_CONVERSATION = "chat:join"
    const val LEAVE_CONVERSATION = "chat:leave"
    const val TYPING_START = "chat:typing_start"
    const val TYPING_STOP = "chat:typing_stop"
}

/**
 * Socket events received from server.
 */
enum class SocketEvent(val eventName: String) {
    POSTS_UNREAD_UPDATED("posts_unread_updated"),
    FRIEND_REQUEST_RECEIVED("friend_request_updated"),
    CHAT_MESSAGE_NEW("chat:message.new"),
    CHAT_MESSAGE_DELETED("chat:message.deleted"),
    CHAT_TYPING_START("chat:typing.start"),
    CHAT_TYPING_STOP("chat:typing.stop"),
    CHAT_MESSAGE_READ("chat:message.read"),
    CHAT_MESSAGE_REACTION_UPDATED("chat:message.reaction_updated"),
    CHAT_CONVERSATION_UPDATED("conversation_updated"),
    CHAT_CONVERSATION_DELETED("conversation_deleted"),
    UNKNOWN("unknown");

    companion object {
        fun from(name: String): SocketEvent =
            entries.firstOrNull { it.eventName == name } ?: UNKNOWN
    }
}
