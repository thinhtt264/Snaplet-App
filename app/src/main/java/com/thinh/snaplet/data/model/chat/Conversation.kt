package com.thinh.snaplet.data.model.chat

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.local.entity.ConversationEntity
import java.util.Date

data class ConversationPartner(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("displayName")
    val displayName: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String?
)

data class LastMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("text")
    val text: String?,
    @SerializedName("mediaKey")
    val mediaKey: String?,
    @SerializedName("mediaUrl")
    val mediaUrl: String?,
    @SerializedName("mimeType")
    val mimeType: String?,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
    @SerializedName("createdAt")
    val createdAt: Date
) {
    /**
     * Compute message type based on mimeType.
     * - image/jpeg, image/jpg, image/png, image/webp → image
     * - image/gif → gif
     * - null or other → text
     */
    val type: String
        get() = when {
            mimeType == null -> "text"
            mimeType.lowercase() in listOf("image/jpeg", "image/jpg", "image/png", "image/webp") -> "image"
            mimeType.lowercase() == "image/gif" -> "gif"
            else -> "text"
        }
}

data class Conversation(
    @SerializedName("id")
    val id: String,
    @SerializedName("partner")
    val partner: ConversationPartner,
    @SerializedName("lastMessage")
    val lastMessage: LastMessage?,
    @SerializedName("partnerLastSeenAt")
    val partnerLastReadAt: Date?,
    @SerializedName("myLastSeenAt")
    val myLastReadAt: Date?,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("syncUpdatedAt")
    val syncUpdatedAt: Date,
)

@Keep
data class ConversationUpdatedEvent(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("lastMessageAt")
    val lastMessageAt: Date,
    @SerializedName("lastMessageSenderId")
    val lastMessageSenderId: String,
    @SerializedName("lastMessageText")
    val lastMessageText: String,
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    participantId = partner.id,
    participantName = partner.displayName,
    participantAvatarUrl = partner.avatarUrl,
    lastMessageId = lastMessage?.id,
    lastMessageText = lastMessage?.text,
    lastMessageType = lastMessage?.type,
    isLastMessageDeleted = lastMessage?.isDeleted ?: false,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageAt = lastMessage?.createdAt?.time,
    myLastSeenAt = myLastReadAt?.time,
    partnerLastSeenAt = partnerLastReadAt?.time,
    updatedAt = syncUpdatedAt.time,
)