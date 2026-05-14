package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.screens.chat.PARTNER_TYPING_IDLE_MS
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.ui.theme.Typography
import kotlin.math.PI
import kotlin.math.sin

private val ChatBg = Color(0xFF0D0D0D)

private val TypingDotLayoutSize = 6.dp

/**
 * One full wave across the three dots; derived from [PARTNER_TYPING_IDLE_MS] so UI timing stays tied to chat typing rules.
 */
private val TypingDotsCycleMillis =
    (PARTNER_TYPING_IDLE_MS / 4).toInt().coerceAtLeast(MotionTokens.Normal)

@Composable
fun ChatHeader(
    name: String,
    avatarUrl: String?,
    isOnline: Boolean,
    isPartnerTyping: Boolean,
    onNavigateBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ChatBg)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                tint = MaterialTheme.colorScheme.onBackground,
            ),
            iconSize = 28.dp,
            onClick = onNavigateBack,
            iconDecoration = IconDecoration(padding = 12.dp),
        )

        // Avatar with online presence dot
        Box(modifier = Modifier.size(42.dp)) {
            Avatar(
                avatarUrl = avatarUrl,
                firstName = name,
                size = 42.dp,
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(1.5.dp, ChatBg, CircleShape)
                        .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = name,
                typography = Typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ChatPartnerPresenceLabel(
                isOnline = isOnline,
                isPartnerTyping = isPartnerTyping,
            )
        }

        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.Default.MoreVert,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = onMore,
            containerColor = Color.Transparent,
            iconDecoration = IconDecoration(padding = 12.dp),
        )
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun ChatPartnerPresenceLabel(
    isOnline: Boolean,
    isPartnerTyping: Boolean,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val offlineMuted = Color.White.copy(alpha = 0.4f)
    val onlineText = stringResource(R.string.chat_status_online)
    val offlineText = stringResource(R.string.chat_status_offline)
    val typingPrefix = stringResource(R.string.chat_status_typing)
    val typingCd = stringResource(R.string.chat_partner_typing_cd)

    SubcomposeLayout { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        val onlinePlaceable = subcompose("measure_online") {
            BaseText(
                text = onlineText,
                typography = Typography.labelSmall,
                color = tertiary,
            )
        }.first().measure(loose)

        val offlinePlaceable = subcompose("measure_offline") {
            BaseText(
                text = offlineText,
                typography = Typography.labelSmall,
                color = offlineMuted,
            )
        }.first().measure(loose)

        val typingPlaceable = subcompose("measure_typing") {
            ChatPartnerTypingRowContent(
                tertiary = tertiary,
                typingPrefix = typingPrefix,
                typingCd = typingCd,
                progress = 0f,
                includeSemantics = false,
            )
        }.first().measure(loose)

        val slotWidth = maxOf(
            onlinePlaceable.width,
            offlinePlaceable.width,
            typingPlaceable.width,
        )
        val slotHeight = maxOf(
            onlinePlaceable.height,
            offlinePlaceable.height,
            typingPlaceable.height,
        )

        val contentMeasurable = subcompose("visible") {
            when {
                isPartnerTyping -> ChatPartnerTypingRow(
                    tertiary = tertiary,
                    typingPrefix = typingPrefix,
                    typingCd = typingCd,
                )

                isOnline -> BaseText(
                    text = onlineText,
                    typography = Typography.labelSmall,
                    color = tertiary,
                )

                else -> BaseText(
                    text = offlineText,
                    typography = Typography.labelSmall,
                    color = offlineMuted,
                )
            }
        }.first()

        val contentPlaceable = contentMeasurable.measure(
            Constraints(
                minWidth = slotWidth,
                maxWidth = slotWidth,
                minHeight = 0,
                maxHeight = constraints.maxHeight,
            ),
        )

        val layoutHeight = maxOf(slotHeight, contentPlaceable.height)
        layout(slotWidth, layoutHeight) {
            val y = (layoutHeight - contentPlaceable.height) / 2
            contentPlaceable.place(0, y)
        }
    }
}

@Composable
private fun ChatPartnerTypingRow(
    tertiary: Color,
    typingPrefix: String,
    typingCd: String,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chat_header_typing")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = TypingDotsCycleMillis,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "typing_wave",
    )
    ChatPartnerTypingRowContent(
        tertiary = tertiary,
        typingPrefix = typingPrefix,
        typingCd = typingCd,
        progress = progress,
    )
}

@Composable
private fun ChatPartnerTypingRowContent(
    tertiary: Color,
    typingPrefix: String,
    typingCd: String,
    progress: Float,
    includeSemantics: Boolean = true,
) {
    Row(
        modifier = Modifier.then(
            if (includeSemantics) Modifier.semantics { contentDescription = typingCd }
            else Modifier,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { index ->
                val angle = progress * 2.0 * PI - index * (2.0 * PI / 3.0)
                val pulse = (sin(angle) * 0.5 + 0.5).toFloat().coerceIn(0f, 1f)
                // Pulse visible size (graphicsLayer after clip/background so scale isn’t clipped away).
                val scalePulse = 0.72f + pulse * 0.44f
                // Narrow alpha range so dim state isn’t too dark vs bright peak.
                val dotAlpha = 0.56f + pulse * 0.24f
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 3.dp)
                        .size(TypingDotLayoutSize)
                        .clip(CircleShape)
                        .background(tertiary.copy(alpha = dotAlpha))
                        .graphicsLayer {
                            scaleX = scalePulse
                            scaleY = scalePulse
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        BaseText(
            text = typingPrefix,
            typography = Typography.labelSmall,
            color = tertiary,
        )
    }
}
