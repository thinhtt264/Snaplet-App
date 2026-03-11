package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.common.CommonImages
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.MotionTokens
import com.thinh.snaplet.ui.theme.Typography
import pressScaleClickable

private val ICON_SIZE = 36.dp

@Composable
fun TopAction(
    modifier: Modifier = Modifier,
    hasCaptureImage: Boolean,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onChatClick: () -> Unit,
    avatarUrl: String,
    friendsCount: Int? = null
) {
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

                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape
                        )
                        .pressScaleClickable(onClick = onFriendsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        if (friendsCount != null) {
                            BaseText(
                                text = friendsCount.toString(),
                                color = Color.White,
                                typography = Typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        BaseText(
                            text = stringResource(R.string.friends),
                            color = Color.White,
                            typography = Typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                AppIconButton(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = onChatClick,
                    iconSize = ICON_SIZE,
                    iconDecoration = IconDecoration(padding = 6.dp),
                    icon = IconSpec.Painter(
                        painterResource(CommonImages.ChatIcon),
                        tint = Color.White
                    )
                )
            }
        }
    }
}