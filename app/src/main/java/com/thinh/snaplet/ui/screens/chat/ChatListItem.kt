package com.thinh.snaplet.ui.screens.chat

import com.thinh.snaplet.data.local.entity.MessageEntity

sealed class ChatListItem {
    data class MessageItem(val message: MessageEntity) : ChatListItem()
    data class DateSeparator(val dateMillis: Long) : ChatListItem()
}
