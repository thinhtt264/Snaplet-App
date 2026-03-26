package com.thinh.snaplet.ui.widget

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thinh.snaplet.MainActivity
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.theme.WidgetAvatarPlaceholder
import com.thinh.snaplet.ui.theme.WidgetStatePanelBackground
import com.thinh.snaplet.ui.theme.onBackground_dark
import com.thinh.snaplet.ui.theme.onPrimaryContainer_light
import com.thinh.snaplet.ui.theme.primary_dark

@Composable
fun SnapletWidgetContent(
    data: WidgetDisplayData,
    modifier: GlanceModifier = GlanceModifier,
) {
    val glanceAppFontFamily = FontFamily.Serif

    Box(
        modifier = modifier.fillMaxSize().background(glanceColor(Color.Transparent))
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        when {
            data.isError -> WidgetErrorState(fontFamily = glanceAppFontFamily)
            data.isLoading -> WidgetErrorState(
                fontFamily = glanceAppFontFamily,
                showCenterContent = true,
            )
            else -> WidgetPostState(
                data = data,
                fontFamily = glanceAppFontFamily
            )
        }
    }
}

@Composable
private fun WidgetErrorState(
    fontFamily: FontFamily,
    showCenterContent: Boolean = true,
) {
    val context = androidx.glance.LocalContext.current
    val widgetSize = LocalSize.current
    val squareEdge = min(widgetSize.width, widgetSize.height)
    val avatarSize = squareEdge * 0.5f
    val ringThickness = 2.dp

    Box(
        modifier = GlanceModifier.size(squareEdge)
            .background(glanceColor(WidgetStatePanelBackground))
            .cornerRadius(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showCenterContent) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(avatarSize + ringThickness * 3)
                        .background(glanceColor(primary_dark))
                        .cornerRadius(999.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(avatarSize)
                            .background(glanceColor(WidgetStatePanelBackground))
                            .cornerRadius(999.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.photo_placeholder),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier
                                .size(avatarSize / 1.9f)
                        )
                    }
                }
                Spacer(modifier = GlanceModifier.height(12.dp))
                Text(
                    text = context.getString(R.string.no_pics_yet),
                    style = TextStyle(
                        color = glanceColor(onBackground_dark),
                        fontSize = 13.sp,
                        fontFamily = fontFamily,
                    ),
                )
            }
        }
    }
}

@Composable
private fun WidgetPostState(
    data: WidgetDisplayData,
    fontFamily: FontFamily
) {
    val context = androidx.glance.LocalContext.current
    val widgetSize = LocalSize.current
    val squareEdge = min(widgetSize.width, widgetSize.height)
    var postBitmap by remember(data.postImageUrl) { mutableStateOf<Bitmap?>(null) }
    var avatarBitmap by remember(data.senderAvatarUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data.postImageUrl) {
        postBitmap = data.postImageUrl?.let {
            loadWidgetBitmap(context, it, maxEdgePx = WidgetImageLoadDefaults.POST_MAX_EDGE_PX)
        }
    }
    LaunchedEffect(data.senderAvatarUrl) {
        avatarBitmap = data.senderAvatarUrl?.let { loadWidgetBitmap(context, it) }
    }


    Box(modifier = GlanceModifier.size(squareEdge)) {
        if (postBitmap != null) {
            Image(
                provider = ImageProvider(postBitmap!!),
                contentDescription = "Post image",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp),
            )
        } else {
            // Placeholder to avoid transparent surface while bitmap is still loading.
            WidgetErrorState(
                fontFamily = fontFamily,
                showCenterContent = false,
            )
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
        ) {
            Box(modifier = GlanceModifier.padding(10.dp)) {
                if (avatarBitmap != null) {
                    Image(
                        provider = ImageProvider(avatarBitmap!!),
                        contentDescription = "Sender avatar",
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp),
                    )
                } else {
                    Box(
                        modifier = GlanceModifier.size(18.dp).cornerRadius(18.dp)
                            .background(glanceColor(WidgetAvatarPlaceholder)),
                    ) {}
                }
            }
        }

        if (data.unreadCount > 0) {
            val badgeText = if (data.unreadCount > 9) "9+" else data.unreadCount.toString()
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    modifier = GlanceModifier.background(glanceColor(primary_dark))
                        .cornerRadius(999.dp).padding(vertical = 4.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = badgeText,
                        style = TextStyle(
                            color = glanceColor(onPrimaryContainer_light),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = fontFamily,
                        ),
                    )
                }
            }
        }

        val caption = data.postCaption.orEmpty()
        if (caption.isNotBlank()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = GlanceModifier.fillMaxWidth()
                            .background(glanceColor(Color.Black.copy(alpha = 0.5f)))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = caption,
                            maxLines = 2,
                            style = TextStyle(
                                color = glanceColor(onBackground_dark),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontFamily = fontFamily,
                            ),
                        )
                    }
                }
            }
        }
    }
}


private fun glanceColor(color: Color): ColorProvider =
    androidx.glance.color.ColorProvider(day = color, night = color)