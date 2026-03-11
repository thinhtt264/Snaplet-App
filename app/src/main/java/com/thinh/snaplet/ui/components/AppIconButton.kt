package com.thinh.snaplet.ui.components

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.thinh.snaplet.ui.components.image.AsyncImage
import com.thinh.snaplet.ui.components.image.ErrorPlaceholderConfig
import com.thinh.snaplet.ui.components.image.ErrorStateConfig
import com.thinh.snaplet.ui.components.image.ImageSize
import com.thinh.snaplet.ui.components.image.LoadingStateConfig
import pressScaleClickable

sealed interface IconSpec {
    data class Vector(val imageVector: ImageVector, val tint: Color = Color.Unspecified) : IconSpec

    /** Resource drawable from R.drawable.xxx */
    data class Drawable(@DrawableRes val resId: Int) : IconSpec

    /** Runtime drawable (e.g. app icon from PackageManager). Shown via ImageView – no manual bitmap conversion. */
    data class AndroidDrawable(val drawable: android.graphics.drawable.Drawable) : IconSpec
    data class Painter(
        val painter: androidx.compose.ui.graphics.painter.Painter,
        val tint: Color = Color.Unspecified
    ) : IconSpec

    /** Image loaded from URL via AsyncImage (e.g. avatar). */
    data class Url(
        val url: String, val fallbackIcon: ImageVector? = null, val tint: Color = Color.Unspecified
    ) : IconSpec
}

@Composable
fun AppIconButton(
    modifier: Modifier = Modifier,
    icon: IconSpec,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    iconSize: Dp = 24.dp,
    shape: Shape = CircleShape,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    iconDecoration: IconDecoration = IconDecoration(),
) {
    Surface(
        modifier = modifier.pressScaleClickable(onClick = onClick, enabled = enabled && !loading),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = iconDecoration.backgroundColor, shape = iconDecoration.shape
                )
                .padding(iconDecoration.padding), contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(iconSize)
                        .padding(2.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                when (icon) {
                    is IconSpec.Vector -> {
                        Icon(
                            imageVector = icon.imageVector,
                            contentDescription = null,
                            tint = icon.tint,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    is IconSpec.Drawable -> {
                        Icon(
                            painter = painterResource(icon.resId),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    is IconSpec.AndroidDrawable -> {
                        AndroidView(modifier = Modifier.size(iconSize), factory = { ctx ->
                            ImageView(ctx).apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                            }
                        }, update = { it.setImageDrawable(icon.drawable) })
                    }

                    is IconSpec.Painter -> {
                        Icon(
                            painter = icon.painter,
                            contentDescription = null,
                            tint = icon.tint,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    is IconSpec.Url -> {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(shape)
                        ) {
                            AsyncImage(
                                imageUrl = icon.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                resizeSize = ImageSize.Thumbnail,
                                loadingConfig = LoadingStateConfig.None,
                                errorConfig = ErrorStateConfig(
                                    placeholder = icon.fallbackIcon?.let {
                                        ErrorPlaceholderConfig.WithIcon(
                                            imageVector = it,
                                            tint = icon.tint
                                        )
                                    } ?: ErrorPlaceholderConfig.None
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

data class IconDecoration(
    val backgroundColor: Color = Color.Transparent,
    val shape: Shape = CircleShape,
    val padding: Dp = 6.dp
)