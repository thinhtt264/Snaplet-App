package com.thinh.snaplet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thinh.snaplet.ui.screens.conversation_list.ConversationUiModel
import java.util.Date

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
    val lastMessageAt: Date?,
    val myLastSeenAt: Date?,
    val partnerLastSeenAt: Date?,
    val updatedAt: Date,
    val isRestricted: Boolean = false,
)

data class ConversationUpdatedAtProjection(
    val id: String,
    val updatedAt: Date,
    val myLastSeenAt: Date?,
    val partnerLastSeenAt: Date?,
)

data class ConversationLastMessageStatusProjection(
    val conversationId: String,
    val status: String?,
)

fun ConversationEntity.toUiModel(
    myUserId: String?,
    lastMessageStatus: String? = null,
    isPartnerOnline: Boolean = false,
): ConversationUiModel {
    val lastMessageMs = lastMessageAt?.time ?: 0L
    val myLastSeenMs = myLastSeenAt?.time ?: 0L
    val partnerLastSeenMs = partnerLastSeenAt?.time ?: 0L
    val hasUnread = lastMessageSenderId != myUserId && lastMessageMs > myLastSeenMs
    val isLastMessageMine = lastMessageSenderId == myUserId
    val partnerHasSeen = partnerLastSeenMs >= (lastMessageAt?.time ?: 1L)
    return ConversationUiModel(
        id = id,
        participantId = participantId,
        participantName = participantName,
        participantAvatarUrl = participantAvatarUrl,
        lastMessageText = lastMessageText,
        lastMessageType = lastMessageType,
        isLastMessageDeleted = isLastMessageDeleted,
        lastMessageAt = lastMessageAt,
        myLastSeenAt = myLastSeenAt,
        partnerLastSeenAt = partnerLastSeenAt,
        lastMessageStatus = lastMessageStatus,
        hasUnread = hasUnread,
        isLastMessageMine = isLastMessageMine,
        partnerHasSeen = partnerHasSeen,
        isPartnerOnline = isPartnerOnline,
    )
}
