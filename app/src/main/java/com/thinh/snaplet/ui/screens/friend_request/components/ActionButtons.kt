package com.thinh.snaplet.ui.screens.friend_request.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.components.AppText
import com.thinh.snaplet.ui.theme.GoldenPollen

/**
 * ActionButtons - Internal component for bottom action button
 * 
 * Features:
 * - Single centered close/dismiss button
 * - Clean, minimal design matching UI mockup
 * 
 * @param onDismiss Callback when close button is tapped
 * @param modifier Optional modifier
 */
@Composable
internal fun ActionButtons(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Center close button (primary action)
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(
                    width = 3.dp,
                    color = GoldenPollen,
                    shape = CircleShape
                )
                .background(
                    color = Color(0xFF3D3D3D),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxSize()
            ) {
                AppText(
                    text = "Đóng",
                    typography = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

