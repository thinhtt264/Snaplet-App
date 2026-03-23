package com.thinh.snaplet.ui.screens.friend_request.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import pressScaleClickable

@Composable
internal fun ActionButtons(
    onDismiss: () -> Unit, modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = shape
            )
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(16.dp)
            .pressScaleClickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        BaseText(
            text = stringResource(R.string.close),
            typography = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}