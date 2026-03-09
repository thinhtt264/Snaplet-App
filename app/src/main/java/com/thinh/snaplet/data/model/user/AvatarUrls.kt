package com.thinh.snaplet.data.model.user

import com.google.gson.annotations.SerializedName

data class AvatarUrls(
    @SerializedName("original")
    val original: String = "",

    @SerializedName("xs")
    val xs: String = "",

    @SerializedName("sm")
    val sm: String = "",

    @SerializedName("md")
    val md: String = "",
) {
    fun forThumbnail(): String = xs.ifBlank { original }

    fun forMedium(): String = sm.ifBlank { original }

    fun forHeader(): String = md.ifBlank { original }
}