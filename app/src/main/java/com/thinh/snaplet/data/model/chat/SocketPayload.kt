package com.thinh.snaplet.data.model.chat

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class TypingPayload(
    @SerializedName("userId")
    val userId: String,
)

@Keep
data class MarkReadPayload(
    @SerializedName("conversationId")
    val conversationId: String,
    @SerializedName("messageId")
    val messageId: String,
)

@Keep
data class TypingSocketPayload(
    @SerializedName("conversationId")
    val conversationId: String,
)

@Keep
data class PartnerPresencePayload(
    @SerializedName("userId")
    val userId: String,
)
