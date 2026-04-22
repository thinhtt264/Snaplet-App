package com.thinh.snaplet.data.model.chat

import com.google.gson.annotations.SerializedName
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
    val createdAt: Date
)

data class ConversationUpdatedEvent(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("lastMessage")
    val lastMessage: LastMessage?,
    @SerializedName("partnerLastReadAt")
    val partnerLastReadAt: Date?,
    @SerializedName("myLastReadAt")
    val myLastReadAt: Date?,
)
