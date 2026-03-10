package com.thinh.snaplet.data.model.user

import com.google.gson.annotations.SerializedName

data class AvatarUploadRequest(
    @SerializedName("mimeType")
    val mimeType: String,

    @SerializedName("size")
    val size: Long,
)

data class AvatarUploadRequestResponse(
    @SerializedName("uploadUrl")
    val uploadUrl: String,

    @SerializedName("key")
    val key: String,

    @SerializedName("maxSizeBytes")
    val maxSizeBytes: Long,
)

data class ConfirmAvatarUploadRequest(
    @SerializedName("key")
    val key: String,
)

