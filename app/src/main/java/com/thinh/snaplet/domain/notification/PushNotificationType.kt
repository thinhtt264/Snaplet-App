package com.thinh.snaplet.domain.notification

enum class PushNotificationType {
    POST_REACTION,
    WIDGET_REFRESH,
    NEW_CHAT_MESSAGE,
    NEW_MESSAGE_REACTION,
    UNKNOWN,
    ;

    companion object {
        fun from(rawType: String?): PushNotificationType {
            if (rawType.isNullOrBlank()) return UNKNOWN
            return when (rawType.trim().uppercase()) {
                POST_REACTION.name -> POST_REACTION
                WIDGET_REFRESH.name -> WIDGET_REFRESH
                NEW_CHAT_MESSAGE.name -> NEW_CHAT_MESSAGE
                NEW_MESSAGE_REACTION.name -> NEW_MESSAGE_REACTION
                else -> UNKNOWN
            }
        }
    }
}
