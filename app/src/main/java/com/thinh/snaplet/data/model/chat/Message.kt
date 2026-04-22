package com.thinh.snaplet.data.model.chat

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.Date

data class MessageAttachment(
    @SerializedName("mediaKey")
    val mediaKey: String,
    @SerializedName("mimeType")
    val mimeType: String,
    @SerializedName("width")
    val width: Int?,
    @SerializedName("height")
    val height: Int?
)

data class Message(
    @SerializedName("id")
    val id: String,
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("clientUuid")
    val clientUuid: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("content")
    val content: String?,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
    @SerializedName("replyTo")
    val replyTo: Message?,
    @SerializedName("attachments")
    val attachments: List<MessageAttachment>,
    @SerializedName("pinnedAt")
    val pinnedAt: Date?,
    @SerializedName("createdAt")
    val createdAt: Date
)

data class SendMessageRequest(
    @SerializedName("clientUuid")
    val clientUuid: String,
    @SerializedName("type")
    val type: String = "text",
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("replyToId")
    val replyToId: String? = null,
    @SerializedName("attachments")
    val attachments: List<MessageAttachment>? = null
)

object MessageType {
    const val TEXT = "text"
    const val IMAGE = "image"
}

@Keep
data class MessageReadEvent(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("messageCreatedAt")
    val messageCreatedAt: Date,
    @SerializedName("readAt")
    val readAt: Date,
)
