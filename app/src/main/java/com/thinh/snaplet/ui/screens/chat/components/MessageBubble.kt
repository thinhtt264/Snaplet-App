package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageType
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.to24HourTime

private val BubbleTheirs = Color(0xFF1E2020)
private const val BUBBLE_MAX_WIDTH_FRACTION = 0.75f
private val BUBBLE_CORNER = 16.dp
private val BUBBLE_CORNER_SMALL = 4.dp

internal val BUBBLE_VERTICAL_PADDING = 8.dp

private val ICON_SIZE = 14.dp

enum class BubblePosition { FIRST, MIDDLE, LAST, SINGLE }

@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    isPending: Boolean,
    isError: Boolean,
    showSeenTick: Boolean,
    position: BubblePosition,
    onRetry: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current

    val screenWidthDp = with(density) {
        windowSize.width.toDp()
    }

    val bubbleColor = if (isMine) cs.onBackground else BubbleTheirs
    val textColor = if (isMine) Color(0xFF0D0D0D) else Color.White
    val metaColor = if (isMine) cs.background.copy(alpha = 0.6f)
    else cs.onBackground.copy(alpha = 0.6f)

    val translationYPx = with(density) { 6.dp.toPx() }

    val shape = if (isMine) mineShape(position) else theirShape(position)

    val isImageMessage = message.messageType == MessageType.IMAGE
    val resolvePadding = if (isImageMessage) PaddingValues(0.dp)
    else PaddingValues(horizontal = 12.dp, vertical = BUBBLE_VERTICAL_PADDING)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = screenWidthDp * BUBBLE_MAX_WIDTH_FRACTION)
                .clip(shape)
                .background(bubbleColor)
                .padding(resolvePadding)
        ) {
            when {
                message.isDeleted -> {
                    BaseText(
                        text = stringResource(R.string.conversation_deleted_message),
                        color = textColor.copy(alpha = 0.5f),
                        typography = Typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                    )
                }

                isImageMessage -> {
                    val media = message.media ?: return@Column
                    val ratio = media.width.toFloat() / media.height.toFloat()

                    Box(modifier = Modifier.padding(bottom = BUBBLE_VERTICAL_PADDING)) {
                        MessageImageContent(
                            modifier = Modifier.fillMaxWidth(),
                            imageUrl = media.urls?.md.orEmpty(),
                            ratio = ratio,
                            bubbleColor = bubbleColor,
                            textColor = textColor,
                            caption = message.text,
                            onRetry = onRetry,
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp)
                                .graphicsLayer { translationY = translationYPx },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            BaseText(
                                text = message.createdAt.to24HourTime(),
                                color = metaColor,
                                typography = Typography.labelSmall,
                            )
                            if (isMine) MessageStatusIcon(
                                isPending = isPending,
                                showError = isError,
                                showSeenTick = showSeenTick,
                                iconSize = ICON_SIZE,
                            ) else Spacer(Modifier.height(ICON_SIZE))
                        }
                    }
                }

                message.messageType == MessageType.GIF -> {
                    BaseText(
                        text = stringResource(R.string.conversation_message_photo),
                        color = textColor,
                        typography = Typography.bodyMedium,
                    )
                }

                else -> {
                    Row(verticalAlignment = Alignment.Bottom) {
                        BaseText(
                            text = message.text.orEmpty(),
                            color = textColor,
                            typography = Typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Row(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .graphicsLayer { translationY = translationYPx },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            BaseText(
                                text = message.createdAt.to24HourTime(),
                                color = metaColor,
                                typography = Typography.labelSmall,
                            )

                            if (isMine) MessageStatusIcon(
                                isPending = isPending,
                                showError = isError,
                                showSeenTick = showSeenTick,
                                iconSize = ICON_SIZE,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun mineShape(position: BubblePosition) = when (position) {
    BubblePosition.FIRST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER_SMALL, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.MIDDLE -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER_SMALL,
        bottomEnd = BUBBLE_CORNER_SMALL, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.LAST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER_SMALL,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.SINGLE -> RoundedCornerShape(BUBBLE_CORNER)
}

private fun theirShape(position: BubblePosition) = when (position) {
    BubblePosition.FIRST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER_SMALL,
    )

    BubblePosition.MIDDLE -> RoundedCornerShape(
        topStart = BUBBLE_CORNER_SMALL, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER_SMALL,
    )

    BubblePosition.LAST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER_SMALL, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.SINGLE -> RoundedCornerShape(BUBBLE_CORNER)
}