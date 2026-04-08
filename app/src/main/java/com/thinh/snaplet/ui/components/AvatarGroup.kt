package com.thinh.snaplet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Model ────────────────────────────────────────────────────

@Stable
data class AvatarGroupItem(
    val avatarUrl: String?,
    val firstName: String,
)

// ─── Component ────────────────────────────────────────────────

private val DEFAULT_AVATAR_SIZE = 28.dp
private const val DEFAULT_OVERLAP_FRACTION = 0.30f

@Composable
fun AvatarGroup(
    items: List<AvatarGroupItem>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = DEFAULT_AVATAR_SIZE,
    overlapFraction: Float = DEFAULT_OVERLAP_FRACTION,
) {
    if (items.isEmpty()) return

    SubcomposeLayout(modifier = modifier) { constraints ->
        val avatarSizePx = avatarSize.toPx()
        val stepPx = avatarSizePx * (1f - overlapFraction)

        val availableWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth.toFloat()
        } else {
            avatarSizePx * items.size
        }

        val maxVisible = if (availableWidth <= avatarSizePx) 1
        else ((availableWidth - avatarSizePx) / stepPx + 1).toInt().coerceAtLeast(1)

        val hasOverflow = items.size > maxVisible
        val visibleCount = if (hasOverflow) (maxVisible - 1).coerceAtLeast(1) else items.size
        val overflowCount = items.size - visibleCount
        val slotCount = visibleCount + if (hasOverflow) 1 else 0
        val totalWidth = (avatarSizePx + stepPx * (slotCount - 1)).toInt()
        val totalHeight = avatarSizePx.toInt()

        val childConstraints = Constraints.fixed(avatarSizePx.toInt(), avatarSizePx.toInt())

        val avatarPlaceables = items.take(visibleCount).mapIndexed { index, item ->
            subcompose("avatar_$index") {
                Box(
                    modifier = Modifier.size(avatarSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Avatar(
                        avatarUrl = item.avatarUrl,
                        firstName = item.firstName,
                        size = avatarSize,
                    )
                }
            }.first().measure(childConstraints)
        }

        val overflowPlaceable = if (hasOverflow) {
            subcompose("overflow") {
                AvatarGroupOverflowChip(
                    count = overflowCount,
                    size = avatarSize,
                )
            }.first().measure(childConstraints)
        } else null

        layout(totalWidth, totalHeight) {
            avatarPlaceables.forEachIndexed { index, placeable ->
                val xOffset = (stepPx * index).toInt()
                placeable.placeRelative(x = xOffset, y = 0)
            }
            overflowPlaceable?.let {
                val xOffset = (stepPx * visibleCount).toInt()
                it.placeRelative(x = xOffset, y = 0)
            }
        }
    }
}

// ─── Overflow chip ────────────────────────────────────────────

@Composable
internal fun AvatarGroupOverflowChip(
    count: Int,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val label = if (count > 99) "+99" else "+$count"
        val fontSize = (size.value * 0.28f).sp
        BaseText(
            text = label,
            typography = MaterialTheme.typography.labelSmall.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}