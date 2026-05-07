package com.thinh.snaplet.data.model.chat

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.local.entity.MessageStatus
import com.thinh.snaplet.data.model.media.ImageSizes
import java.util.Date

enum class MessageType {
    TEXT,
    IMAGE,
    GIF,
}

data class MessageMedia(
    @SerializedName("urls")
    val urls: ImageSizes?,
    @SerializedName("mimeType")
    val mimeType: String?,
    @SerializedName("width")
    val width: Int = 0,
    @SerializedName("height")
    val height: Int = 0
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
    @SerializedName("text")
    val text: String?,

    @SerializedName("media")
    val media: MessageMedia?,

    @SerializedName("isDeleted")
    val isDeleted: Boolean,
    @SerializedName("replyTo")
    val replyTo: ReplyToMessage?,
    @SerializedName("pinnedAt")
    val pinnedAt: Date?,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("reactions")
    val reactions: List<MessageReaction> = emptyList(),
    // Client-side status — not from server JSON (null for server-sourced messages)
    val status: String? = null,
) {
    /**
     * Compute message type based on mimeType.
     * - image/jpeg, image/jpg, image/png, image/webp → IMAGE
     * - image/gif → GIF
     * - null or other → TEXT
     */
    val messageType: MessageType
        get() = when {
            media?.mimeType == null -> MessageType.TEXT
            media.mimeType.lowercase() in listOf(
                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/webp"
            ) -> MessageType.IMAGE

            media.mimeType.lowercase() == "image/gif" -> MessageType.GIF
            else -> MessageType.TEXT
        }
}

data class ReplyToMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("senderId")
    val senderId: String,
    @SerializedName("text")
    val text: String?,
    @SerializedName("isDeleted")
    val isDeleted: Boolean,
)

data class SendMessageRequest(
    @SerializedName("recipientId")
    val recipientId: String,
    @SerializedName("clientUuid")
    val clientUuid: String,
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("mediaKey")
    val mediaKey: String? = null,
    @SerializedName("mediaUrl")
    val mediaUrl: String? = null,
    @SerializedName("mimeType")
    val mimeType: String? = null,
    @SerializedName("width")
    val width: Int = 0,
    @SerializedName("height")
    val height: Int = 0,
    @SerializedName("replyToId")
    val replyToId: String? = null,
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    type = messageType.name,
    text = text,
    mediaUrl = media?.urls?.md,
    mediaLocalUri = null,
    mediaType = media?.mimeType,
    mediaWidth = media?.width ?: 0,
    mediaHeight = media?.height ?: 0,
    status = MessageStatus.SENT,
    isDeleted = isDeleted,
    createdAt = createdAt,
    serverCreatedAt = createdAt,
    localId = clientUuid,
    reactions = reactions,
)

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
