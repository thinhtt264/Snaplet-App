package com.thinh.snaplet.data.model.chat

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.local.entity.ConversationEntity
import java.util.Date

data class CreateConversationRequest(
    @SerializedName("recipientId")
    val recipientId: String
)

data class CreateConversationData(
    @SerializedName("id")
    val id: String,
    @SerializedName("isNew")
    val isNew: Boolean
)

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
    @SerializedName("type")
    val type: String,
    @SerializedName("content")
    val content: String?,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
    @SerializedName("createdAt")
    val createdAt: Date
)

data class Conversation(
    @SerializedName("id")
    val id: String,
    @SerializedName("partner")
    val partner: ConversationPartner,
    @SerializedName("lastMessage")
    val lastMessage: LastMessage?,
    @SerializedName("partnerLastReadAt")
    val partnerLastReadAt: Date?,
    @SerializedName("myLastReadAt")
    val myLastReadAt: Date?,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("updatedAt")
    val updatedAt: Date,
)

@Keep
data class ConversationUpdatedEvent(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("lastMessageAt")
    val lastMessageAt: Date,
    @SerializedName("lastMessageSenderId")
    val lastMessageSenderId: String,
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    participantId = partner.id,
    participantName = partner.displayName,
    participantAvatarUrl = partner.avatarUrl,
    lastMessageId = lastMessage?.id,
    lastMessageText = lastMessage?.content,
    lastMessageType = lastMessage?.type,
    isLastMessageDeleted = lastMessage?.isDeleted ?: false,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageAt = lastMessage?.createdAt?.time,
    myLastSeenAt = myLastReadAt?.time,
    partnerLastSeenAt = partnerLastReadAt?.time,
    updatedAt = updatedAt.time,
)