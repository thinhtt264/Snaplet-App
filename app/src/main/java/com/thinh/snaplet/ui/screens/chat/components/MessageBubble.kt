package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageType
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.to24HourTime

private val BubbleTheirs = Color(0xFF1E2020)
private const val BUBBLE_MAX_WIDTH_FRACTION = 0.72f
private val BUBBLE_CORNER = 16.dp
private val BUBBLE_CORNER_SMALL = 4.dp

enum class BubblePosition { FIRST, MIDDLE, LAST, SINGLE }

@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    isPending: Boolean,
    showTick: Boolean,
    showReadAvatar: Boolean,
    partnerAvatarUrl: String?,
    partnerName: String,
    position: BubblePosition,
) {
    val screenWidthDp = LocalWindowInfo.current.containerSize.width.dp
    val cs = MaterialTheme.colorScheme

    val bubbleColor = if (isMine) cs.onBackground else BubbleTheirs
    val textColor = if (isMine) Color(0xFF0D0D0D) else Color.White
    val metaColor = if (isMine) cs.background.copy(alpha = 0.6f)
    else cs.onBackground.copy(alpha = 0.6f)

    val shape = if (isMine) mineShape(position) else theirShape(position)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = (screenWidthDp * BUBBLE_MAX_WIDTH_FRACTION))
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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

                message.type == MessageType.IMAGE -> {
                    BaseText(
                        text = stringResource(R.string.conversation_message_photo),
                        color = textColor,
                        typography = Typography.bodyMedium,
                    )
                }

                else -> {
                    BaseText(
                        text = message.content.orEmpty(),
                        color = textColor,
                        typography = Typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                BaseText(
                    text = message.createdAt.to24HourTime(),
                    color = metaColor,
                    typography = Typography.labelSmall,
                )

                if (isMine) {
                    val tickAlpha = if (isPending) 0.3f else 0.5f
                    when {
                        showReadAvatar -> {
                            Avatar(
                                avatarUrl = partnerAvatarUrl,
                                firstName = partnerName,
                                size = 14.dp,
                            )
                        }

                        showTick -> {
                            Icon(
                                imageVector = if (isPending) Icons.Filled.Done else Icons.Filled.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF0D0D0D).copy(alpha = tickAlpha),
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