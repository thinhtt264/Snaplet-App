package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.post.PostAudience
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import pressScaleClickable

/**
 * Visual configuration for [PostAudienceSelector].
 *
 * @param avatarSize         Diameter of each avatar circle in idle state.
 * @param selectedSizeBoost  Extra diameter added when an item is selected.
 * @param borderWidth        Thickness of the selection ring.
 * @param itemSpacing        Horizontal gap between items.
 * @param contentPadding     Padding at both ends of the horizontal list.
 */
data class AudienceSelectorStyle(
    val avatarSize: Dp = 50.dp,
    val selectedSizeBoost: Dp = 4.dp,
    val borderWidth: Dp = 3.dp,
    val itemSpacing: Dp = 4.dp,
    val contentPadding: Dp = 0.dp,
)

/**
 * @param friends       Friends shown after the default chip; users support multi-select (toggle on/off).
 * @param selected      Current selection — drives highlight. [PostAudience.FriendOnly] is exclusive with friend picks.
 * @param onSelect      Emits the next [PostAudience] (friend multi-select or [PostAudience.FriendOnly] for “everyone”).
 * @param style         Visual overrides — see [AudienceSelectorStyle].
 * @param modifier      Modifier applied to the outer [LazyRow].
 * @param everyoneLabel Text below the default chip (API friend-only; typically “everyone” / Tất cả).
 */
@Composable
fun PostAudienceSelector(
    friends: List<RelationshipWithUser>,
    selected: PostAudience,
    onSelect: (PostAudience) -> Unit,
    modifier: Modifier = Modifier,
    style: AudienceSelectorStyle = AudienceSelectorStyle(),
    everyoneLabel: String = "Tất cả",
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
        contentPadding = PaddingValues(horizontal = style.contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ── Default chip — clears all friend picks; dimmed while specific friends are selected ───
        item(key = "__friend_only__") {
            val isSelected = selected is PostAudience.FriendOnly
            Column {
                AudienceItem(
                    label = everyoneLabel,
                    isSelected = isSelected,
                    style = style,
                    onClick = { onSelect(PostAudience.FriendOnly) },
                    showOwnBorder = true,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = everyoneLabel,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // ── Friend chips (multi-select). While “everyone” is active, friends look inactive
        items(items = friends, key = { it.userId }) { friend ->
            val isFriendSelected = when (selected) {
                is PostAudience.FriendOnly -> false
                is PostAudience.SelectedFriends -> selected.friends.any { it.userId == friend.userId }
            }

            Column {
                AudienceItem(
                    label = friend.firstName,
                    isSelected = isFriendSelected,
                    style = style,
                    onClick = {
                        val next = when (val s = selected) {
                            is PostAudience.FriendOnly -> PostAudience.SelectedFriends(listOf(friend))

                            is PostAudience.SelectedFriends -> {
                                val toggled = if (s.friends.any { it.userId == friend.userId }) {
                                    s.friends.filter { it.userId != friend.userId }
                                } else {
                                    s.friends + friend
                                }.distinctBy { it.userId }
                                if (toggled.isEmpty()) {
                                    PostAudience.FriendOnly
                                } else {
                                    PostAudience.SelectedFriends(toggled)
                                }
                            }
                        }
                        onSelect(next)
                    },
                    showOwnBorder = false,
                ) {
                    Avatar(
                        modifier = Modifier.fillMaxSize(),
                        avatarUrl = friend.avatarUrls.forThumbnail(),
                        firstName = friend.firstName,
                        isConnectedUser = isFriendSelected,
                        borderWidth = style.borderWidth,
                        contentPadding = style.borderWidth + 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Generic item shell: handles size animation, label color, and column width.
 * Border rendering is intentionally left to [content] so Avatar can own its
 * own ring (via isConnectedUser) without producing a double border.
 *
 * @param showOwnBorder  When true the shell draws the selection ring itself
 *                       (used for the default friend-only chip which has no Avatar child).
 */
@Composable
private fun AudienceItem(
    label: String,
    isSelected: Boolean,
    style: AudienceSelectorStyle,
    onClick: () -> Unit,
    showOwnBorder: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val avatarSize by animateDpAsState(
        targetValue = if (isSelected) style.avatarSize + style.selectedSizeBoost else style.avatarSize,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "avatarSize",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceBright
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "borderColor",
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "labelColor",
    )

    val itemWidth = style.avatarSize + style.selectedSizeBoost + 8.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(itemWidth)
            .pressScaleClickable(onClick = onClick),
    ) {
        val boxModifier = if (showOwnBorder) {
            Modifier
                .size(avatarSize - style.borderWidth / 2)
                .border(BorderStroke(style.borderWidth, borderColor), CircleShape)
                .padding(style.borderWidth + 2.dp)
                .clip(CircleShape)
        } else {
            Modifier.size(avatarSize)
        }

        Box(modifier = boxModifier, content = content)

        BaseText(
            text = label,
            typography = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}