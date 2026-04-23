package com.thinh.snaplet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thinh.snaplet.ui.screens.conversation_list.ConversationUiModel

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val participantId: String,
    val participantName: String,
    val participantAvatarUrl: String?,
    val lastMessageId: String?,
    val lastMessageText: String?,
    val lastMessageType: String?,
    val isLastMessageDeleted: Boolean,
    val lastMessageSenderId: String?,
    val lastMessageAt: Long?,
    val myLastSeenAt: Long?,
    val partnerLastSeenAt: Long?,
    val updatedAt: Long,
)

data class ConversationUpdatedAtProjection(
    val id: String,
    val updatedAt: Long,
)

fun ConversationEntity.toUiModel(myUserId: String?): ConversationUiModel {
    val hasUnread = lastMessageSenderId != myUserId &&
            (lastMessageAt ?: 0L) > (myLastSeenAt ?: 0L)
    val isLastMessageMine = lastMessageSenderId == myUserId
    val partnerHasSeen = (partnerLastSeenAt ?: 0L) >= (lastMessageAt ?: 1L)
    return ConversationUiModel(
        id = id,
        participantName = participantName,
        participantAvatarUrl = participantAvatarUrl,
        lastMessageText = lastMessageText,
        lastMessageType = lastMessageType,
        isLastMessageDeleted = isLastMessageDeleted,
        lastMessageAt = lastMessageAt,
        myLastSeenAt = myLastSeenAt,
        partnerLastSeenAt = partnerLastSeenAt,
        hasUnread = hasUnread,
        isLastMessageMine = isLastMessageMine,
        partnerHasSeen = partnerHasSeen,
    )
}
