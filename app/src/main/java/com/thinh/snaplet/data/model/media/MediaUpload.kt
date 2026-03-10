package com.thinh.snaplet.data.model.media

import com.google.gson.annotations.SerializedName

data class UploadRequestItem(
    @SerializedName("mimeType")
    val mimeType: String,
    
    @SerializedName("size")
    val size: Long,
    
    @SerializedName("transform")
    val transform: ImageTransform? = null
)

data class RequestUploadRequest(
    @SerializedName("items")
    val items: List<UploadRequestItem>
)

data class UploadRequestResponse(
    @SerializedName("mediaId")
    val mediaId: String,
    
    @SerializedName("uploadUrl")
    val uploadUrl: String,
    
    @SerializedName("expiresIn")
    val expiresIn: Int
)

data class UploadRequestData(
    @SerializedName("data")
    val data: List<UploadRequestResponse>
)

data class MediaConfirmUploadRequest(
    @SerializedName("mediaIds")
    val mediaIds: List<String>
)

data class ConfirmUploadMedia(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("mimeType")
    val mimeType: String,
    
    @SerializedName("originalUrl")
    val originalUrl: String,
    
    @SerializedName("transform")
    val transform: ImageTransform? = null,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)

data class ConfirmUploadData(
    @SerializedName("media")
    val media: List<ConfirmUploadMedia>,
    
    @SerializedName("message")
    val message: String? = null
)
