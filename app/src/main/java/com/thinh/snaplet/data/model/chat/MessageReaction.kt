package com.thinh.snaplet.data.model.chat

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.thinh.snaplet.data.model.user.UserProfile
import java.util.Date

data class ReactToMessageRequest(
    @SerializedName("emoji")
    val emoji: String,
)

data class MessageReaction(
    @SerializedName("id")
    val id: String,
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("emoji")
    val emoji: String,
    @SerializedName("createdAt")
    val createdAt: Date,
)

data class MessageReactionWithUserInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("emoji")
    val emoji: String,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("user")
    val user: UserProfile,
)

@Keep
data class MessageReactionUpdatedEvent(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("actorId")
    val actorId: String,
    @SerializedName("actorEmoji")
    val actorEmoji: String?,
    @SerializedName("reactions")
    val reactions: List<MessageReaction>,
)
