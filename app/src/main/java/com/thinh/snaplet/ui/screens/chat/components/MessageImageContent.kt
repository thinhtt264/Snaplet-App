package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Typography
import thenIf

@Composable
fun MessageImageContent(
    modifier: Modifier = Modifier,
    imageUrl: String,
    ratio: Float,
    bubbleColor: Color,
    textColor: Color,
    caption: String?,
    onRetry: (() -> Unit)?,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .thenIf(ratio > 0f) { aspectRatio(ratio) },
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Empty -> {
                        MessageImageShimmer(
                            modifier = Modifier.matchParentSize(),
                            baseColor = bubbleColor,
                        )
                    }

                    is AsyncImagePainter.State.Error -> {
                        MessageImageErrorOverlay(
                            modifier = Modifier.matchParentSize(),
                            baseColor = bubbleColor,
                            textColor = textColor,
                            errorText = stringResource(R.string.conversation_image_load_failed),
                            onRetry = onRetry,
                        )
                    }

                    is AsyncImagePainter.State.Success -> {
                        Image(
                            painter = painter,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                }
            }
        }

        caption?.let {
            BaseText(
                text = it,
                color = textColor,
                typography = Typography.bodyMedium,
                modifier = Modifier.padding(
                    start = 12.dp,
                    end = 6.dp,
                    top = BUBBLE_VERTICAL_PADDING
                ),
            )
        }
    }
}

@Composable
private fun MessageImageErrorOverlay(
    modifier: Modifier = Modifier,
    baseColor: Color,
    textColor: Color,
    errorText: String,
    onRetry: (() -> Unit)?,
) {
    Box(
        modifier = modifier.background(baseColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textColor.copy(alpha = 0.8f),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            BaseText(
                text = errorText,
                color = textColor.copy(alpha = 0.7f),
                typography = Typography.labelMedium,
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.18f))
                        .clickable { onRetry() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = textColor,
                    )
                    BaseText(
                        text = stringResource(R.string.retry),
                        color = textColor,
                        typography = Typography.labelMedium,
                    )
                }
            }
        }
    }
}
