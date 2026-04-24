package com.thinh.snaplet.ui.screens.chat

import com.thinh.snaplet.data.model.chat.MessageReadEvent

data class IncomingUnreadState(
    val count: Int = 0,
    val newestMessageId: String? = null,
) {
    val displayCount: String get() = if (count > 9) "9+" else count.toString()
    val isVisible: Boolean get() = count > 0
}

data class MessageListState(
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class PartnerState(
    val isTyping: Boolean = false,
    val lastReadEvent: MessageReadEvent? = null,
    val lastReadAtMsFallback: Long? = null,
) {
    val readHorizonMs: Long? get() = lastReadEvent?.messageCreatedAt?.time ?: lastReadAtMsFallback
}

data class ReadTrackingState(
    val isUserAtBottom: Boolean = false,
    val incomingUnread: IncomingUnreadState = IncomingUnreadState(),
    val myLastReadCreatedAtMs: Long? = null,
)

data class ChatUiState(
    val currentUserId: String? = null,
    val draftMessage: String? = null,
    val messageList: MessageListState = MessageListState(),
    val partner: PartnerState = PartnerState(),
    val readTracking: ReadTrackingState = ReadTrackingState(),
)
