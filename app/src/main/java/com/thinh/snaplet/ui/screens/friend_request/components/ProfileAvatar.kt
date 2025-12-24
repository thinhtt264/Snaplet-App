package com.thinh.snaplet.ui.screens.friend_request.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.components.ImageSize
import com.thinh.snaplet.ui.components.OptimizedAsyncImage
import com.thinh.snaplet.ui.theme.Gray

@Composable
internal fun ProfileAvatar(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .border(
                width = 4.dp,
                color = Color(0xFF555555),
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        if (avatarUrl != null) {
            OptimizedAsyncImage(
                imageUrl = avatarUrl,
                contentDescription = "Avatar of $displayName",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                resizeSize = ImageSize.Small,
                showLoadingIndicator = true
            )
        } else {
            // Default avatar with initials
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = displayName.firstOrNull()?.toString()?.uppercase() ?: "?",
                    typography = MaterialTheme.typography.displayLarge,
                    color = Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

