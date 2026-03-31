package com.thinh.snaplet.data.model.post

import com.google.gson.annotations.SerializedName

data class ReactToPostRequest(
    @SerializedName("reactionIcon")
    val reactionIcon: String,
)