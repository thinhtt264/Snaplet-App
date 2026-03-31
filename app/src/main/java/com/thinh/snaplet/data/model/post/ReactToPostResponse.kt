package com.thinh.snaplet.data.model.post

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ReactToPostResponse(
    @SerializedName("postId")
    val postId: String,

    @SerializedName("reactorUserId")
    val reactorUserId: String,

    /**
     * Comma-separated reaction icon history (newest first), up to 3 icons.
     * Example: "🎉,😀,🔥"
     */
    @SerializedName("reactionIcon")
    val reactionIcon: String,

    @SerializedName("updatedAt")
    val updatedAt: Date,
)