package com.thinh.snaplet.ui.screens.chat

import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.model.chat.MessageReactionWithUserInfo
import com.thinh.snaplet.data.model.chat.MessageReadEvent
import java.util.Date

data class IncomingUnreadState(
    val count: Int = 0,
    val newestMessageId: String? = null,
) {
    val displayCount: String get() = if (count > 9) "9+" else count.toString()
    val isVisible: Boolean get() = count > 0
}

data class MessageListState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class PartnerState(
    val isTyping: Boolean = false,
    val lastReadEvent: MessageReadEvent? = null,
    val lastReadAtFallback: Date? = null,
) {
    val readHorizon: Date? get() = lastReadEvent?.messageCreatedAt ?: lastReadAtFallback
}

data class ReadTrackingState(
    val isUserAtBottom: Boolean = false,
    val incomingUnread: IncomingUnreadState = IncomingUnreadState(),
    val myLastReadCreatedAt: Date? = null,
)

data class ChatUiState(
    val currentUserId: String? = null,
    val isPartnerOnline: Boolean = false,
    val draftMessage: String? = null,
    val messageList: MessageListState = MessageListState(),
    val partner: PartnerState = PartnerState(),
    val readTracking: ReadTrackingState = ReadTrackingState(),
    val inspectedMessage: MessageEntity? = null,
    val recentEmojis: List<String> = emptyList(),
    val messageReactionsSheet: MessageReactionsSheetState = MessageReactionsSheetState(),
)

data class MessageReactionsSheetState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val messageId: String = "",
    val reactions: List<MessageReactionWithUserInfo> = emptyList(),
)

/** Hide incoming typing this many ms after the last typing start/stop socket event. */
const val PARTNER_TYPING_IDLE_MS = 3_000L
