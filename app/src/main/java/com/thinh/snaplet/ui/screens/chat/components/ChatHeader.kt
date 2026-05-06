package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.Typography

private val ChatBg = Color(0xFF0D0D0D)

@Composable
fun ChatHeader(
    name: String,
    avatarUrl: String?,
    isOnline: Boolean,
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
            BaseText(
                text = if (isOnline) "Đang hoạt động" else "Không hoạt động",
                typography = Typography.labelSmall,
                color = if (isOnline) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.4f),
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