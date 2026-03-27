package com.thinh.snaplet.ui.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetImageLoadDefaults {
    // 32dp avatar in xxhdpi is ~96px.
    const val AVATAR_MAX_EDGE_PX = 96

    // 2x2 widget surface in xxhdpi is around 330px; keep some headroom for crispness.
    const val POST_MAX_EDGE_PX = 384
}

suspend fun loadWidgetBitmap(
    context: Context,
    url: String,
    maxEdgePx: Int = WidgetImageLoadDefaults.AVATAR_MAX_EDGE_PX,
): Bitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            val imageLoader: ImageLoader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(Size(maxEdgePx, maxEdgePx))
                .build()

            val result = imageLoader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.toBitmap() ?: return@runCatching null
            bitmap.downscaleForRemoteViewsIfNeeded()
        }.getOrNull()
    }
}

private const val REMOTE_VIEWS_SAFE_MAX_EDGE_PX = 512
private const val REMOTE_VIEWS_SAFE_MAX_BYTES = 900_000

/**
 * Keep source quality by default; only shrink if bitmap is too heavy for widget RemoteViews transport.
 */
private fun Bitmap.downscaleForRemoteViewsIfNeeded(): Bitmap {
    val needsResize = byteCount > REMOTE_VIEWS_SAFE_MAX_BYTES ||
            width > REMOTE_VIEWS_SAFE_MAX_EDGE_PX ||
            height > REMOTE_VIEWS_SAFE_MAX_EDGE_PX
    if (!needsResize) return this

    val scale = minOf(
        REMOTE_VIEWS_SAFE_MAX_EDGE_PX.toFloat() / width.toFloat(),
        REMOTE_VIEWS_SAFE_MAX_EDGE_PX.toFloat() / height.toFloat(),
    )
    if (scale >= 1f) return this

    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return this.scale(targetWidth, targetHeight)
}
