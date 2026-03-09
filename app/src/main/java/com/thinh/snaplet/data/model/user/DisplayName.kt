package com.thinh.snaplet.data.model.user

import com.google.gson.annotations.SerializedName

data class UpdateDisplayNameRequest(
    @SerializedName("firstName")
    val firstName: String,

    @SerializedName("lastName")
    val lastName: String,
)