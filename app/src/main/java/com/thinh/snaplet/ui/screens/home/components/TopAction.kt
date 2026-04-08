package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.RelationshipCounts
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.CappedCountBadge
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.ui.theme.Primary80
import com.thinh.snaplet.ui.theme.Typography
import pressScaleClickable

private val ICON_SIZE = 36.dp
private val FILTER_TOOLTIP_HEIGHT = 300.dp
private val FILTER_TOOLTIP_WIDTH = 260.dp

@Composable
fun TopAction(
    modifier: Modifier = Modifier,
    hasCaptureImage: Boolean,
    isCameraPage: Boolean,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onChatClick: () -> Unit,
    avatarUrl: String,
    relationshipCounts: RelationshipCounts? = null,
    myUserId: String?,
    selectedFeedUserId: String?,
    acceptedFriends: List<RelationshipWithUser>,
    onFeedFilterUserSelected: (String?) -> Unit,
    isFeedFilterEnabled: Boolean,
) {
    val pendingForBadge = relationshipCounts?.pendingRequestCount ?: 0

    val isFeedFilterMode = !isCameraPage && isFeedFilterEnabled

    var showFilterTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(isFeedFilterMode) {
        // If we enter a non-ready state (loading/error), ensure tooltip doesn't reappear unexpectedly.
        if (!isFeedFilterMode) showFilterTooltip = false
    }

    val selectedLabel = when (selectedFeedUserId) {
        null -> stringResource(id = R.string.feed_filter_everyone)
        myUserId -> stringResource(id = R.string.you)
        else -> acceptedFriends.firstOrNull { it.userId == selectedFeedUserId }?.displayName
            ?: stringResource(id = R.string.feed_filter_everyone)
    }

    val filterArrowRotation by animateFloatAsState(
        targetValue = if (showFilterTooltip) 180f else 0f,
        animationSpec = tween(durationMillis = MotionTokens.Emphasized),
        label = "feed_filter_arrow_rotation"
    )

    val toggleOrOpen = {
        if (isFeedFilterMode) {
            showFilterTooltip = !showFilterTooltip
        } else {
            onFriendsClick()
        }
    }

    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = !hasCaptureImage,
            enter = fadeIn(animationSpec = tween(MotionTokens.Emphasized)),
            exit = fadeOut(animationSpec = tween(MotionTokens.Emphasized)),
            label = "top_action_content"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppIconButton(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onProfileClick,
                    iconSize = ICON_SIZE,
                    iconDecoration = IconDecoration(padding = 6.dp),
                    icon = if (avatarUrl.isBlank()) {
                        IconSpec.Vector(Icons.Outlined.AccountCircle, tint = Color.White)
                    } else {
                        IconSpec.Url(
                            avatarUrl,
                            fallbackIcon = Icons.Outlined.AccountCircle,
                            tint = Color.White
                        )
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box {
                    var rowHeight by remember { mutableIntStateOf(0) }

                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .onGloballyPositioned { coords ->
                                rowHeight = coords.size.height
                            }
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = if (isFeedFilterMode) RoundedCornerShape(24.dp) else CircleShape
                            )
                            .pressScaleClickable(onClick = toggleOrOpen)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )

                        if (!isFeedFilterMode) {
                            Spacer(modifier = Modifier.width(6.dp))

                            relationshipCounts?.let { counts ->
                                if (counts.acceptedFriendCount > 0) {
                                    BaseText(
                                        text = counts.acceptedFriendCount.toString(),
                                        color = Color.White,
                                        typography = Typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }

                            BaseText(
                                text = stringResource(R.string.friends),
                                color = Color.White,
                                typography = Typography.bodyMedium
                            )
                        } else {
                            Spacer(modifier = Modifier.width(4.dp))
                            BaseText(
                                text = selectedLabel,
                                color = Color.White,
                                typography = Typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer { rotationZ = filterArrowRotation }
                            )
                        }
                    }

                    if (!isFeedFilterMode && pendingForBadge > 0) {
                        CappedCountBadge(
                            count = pendingForBadge,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-8).dp),
                            backgroundColor = Primary80,
                            shape = CircleShape,
                            minSize = 24.dp,
                        )
                    }

                    if (isFeedFilterMode && showFilterTooltip) {
                        Popup(alignment = Alignment.TopCenter) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .pointerInput(Unit) {
                                            detectTapGestures { showFilterTooltip = false }
                                        }
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .graphicsLayer(
                                            translationY = (rowHeight * 1.5 + 16).toFloat()
                                        )
                                        .width(FILTER_TOOLTIP_WIDTH)
                                        .heightIn(max = FILTER_TOOLTIP_HEIGHT)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceBright)
                                        .padding(vertical = 8.dp),
                                ) {
                                    item {
                                        FilterRow(
                                            leading = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Group,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            },
                                            title = stringResource(id = R.string.feed_filter_everyone),
                                            onClick = {
                                                onFeedFilterUserSelected(null)
                                                showFilterTooltip = false
                                            })
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }

                                    item {
                                        FilterRow(
                                            leading = {
                                                Avatar(
                                                    avatarUrl = avatarUrl.ifBlank { null },
                                                    firstName = stringResource(id = R.string.you),
                                                    size = 32.dp
                                                )
                                            },
                                            title = stringResource(id = R.string.you),
                                            onClick = {
                                                onFeedFilterUserSelected(myUserId)
                                                showFilterTooltip = false
                                            })
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }

                                    items(
                                        acceptedFriends, key = { it.userId }) { friend ->
                                        FilterRow(leading = {
                                            Avatar(
                                                avatarUrl = friend.avatarUrls.forThumbnail(),
                                                firstName = friend.firstName.ifBlank { friend.username },
                                                size = 32.dp
                                            )
                                        }, title = friend.displayName, onClick = {
                                            onFeedFilterUserSelected(friend.userId)
                                            showFilterTooltip = false
                                        })
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                AppIconButton(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onChatClick,
                    iconSize = ICON_SIZE,
                    iconDecoration = IconDecoration(padding = 6.dp),
                    icon = IconSpec.Painter(
                        painterResource(CommonImages.ChatIcon), tint = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    leading: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(enabled = true, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            leading()
        }

        Spacer(modifier = Modifier.width(12.dp))

        BaseText(
            text = title,
            color = Color.White.copy(alpha = 0.92f),
            typography = Typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}