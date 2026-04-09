package com.thinh.snaplet.domain.notification

enum class PushNotificationType {
    POST_REACTION,
    WIDGET_REFRESH,
    UNKNOWN,
    ;

    companion object {
        fun from(rawType: String?): PushNotificationType {
            if (rawType.isNullOrBlank()) return UNKNOWN
            return when (rawType.trim().uppercase()) {
                POST_REACTION.name -> POST_REACTION
                WIDGET_REFRESH.name -> WIDGET_REFRESH
                else -> UNKNOWN
            }
        }
    }
}
