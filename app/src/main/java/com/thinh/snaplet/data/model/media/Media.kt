package com.thinh.snaplet.data.model.media

import com.google.gson.annotations.SerializedName

/**
 * Image URLs for different sizes.
 * Any size (xs, sm, md, xl) that is blank falls back to [original].
 * Based on ImageSizeKey enum from backend:
 * - original: URL gốc
 * - XS: 64x64 (1:1) - Thumbnail / Icon
 * - SM: 256x256 (1:1) - Preview / Avatar
 * - MD: 512x512 (1:1) - Standard Square
 * - XL: 768x768 (1:1) - High-Res Square
 */
open class ImageSizes(
    @SerializedName("original")
    val original: String = "",

    @SerializedName("xs")
    private val xsValue: String = "",

    @SerializedName("sm")
    private val smValue: String = "",

    @SerializedName("md")
    private val mdValue: String = "",

    @SerializedName("xl")
    private val xlValue: String = ""
) {
    val xs: String get() = xsValue.ifBlank { original }
    val sm: String get() = smValue.ifBlank { original }
    val md: String get() = mdValue.ifBlank { original }
    val xl: String get() = xlValue.ifBlank { original }
}

data class Media(
    @SerializedName("id")
    val id: String,

    @SerializedName("mimeType")
    val type: String,

    @SerializedName("ownerId")
    val ownerId: String,

    @SerializedName("transform")
    val transform: ImageTransform? = null,

    @SerializedName("images")
    val images: ImageSizes = ImageSizes()
)
