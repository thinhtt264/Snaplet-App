package com.thinh.snaplet.ui.screens.chat

import com.thinh.snaplet.data.model.chat.Message

data class ChatUiState(
    val isLoading: Boolean = true,
    val messages: List<Message> = emptyList(), // newest-first (API order), displayed bottom-up via reverseLayout = true
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val draftMessage: String? = null,
    /** clientUuids of messages shown optimistically but not yet confirmed by the server. */
    val pendingClientUuids: Set<String> = emptySet(),
    /** True while the conversation partner is typing. Auto-cleared after a timeout. */
    val isPartnerTyping: Boolean = false,
    /** ID of the last message the partner has confirmed as read (from chat:message.read). */
    val partnerLastReadMessageId: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore && !isLoading
}
