package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.theme.Typography
import pressScaleClickable

private val ICON_SIZE = 28.dp

@Composable
fun TopAction(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onChatClick: () -> Unit,
    avatarUrl: String,
    friendsCount: Int? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconButton(
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            onClick = onProfileClick,
            iconSize = ICON_SIZE,
            icon = if (avatarUrl.isBlank()) {
                IconSpec.Vector(Icons.Outlined.AccountCircle, tint = Color.White)
            } else IconSpec.Url(
                avatarUrl, fallbackIcon = Icons.Outlined.AccountCircle, tint = Color.White
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondary, shape = CircleShape)
                .pressScaleClickable(onClick = onFriendsClick), contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(all = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(ICON_SIZE)
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
            modifier = Modifier.padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary,
            onClick = onChatClick,
            iconSize = ICON_SIZE,
            icon = IconSpec.Vector(Icons.Outlined.ChatBubbleOutline, tint = Color.White)
        )
    }
}