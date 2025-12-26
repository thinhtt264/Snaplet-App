package com.thinh.snaplet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pressScaleClickable

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    typography: TextStyle = MaterialTheme.typography.titleLarge,
) {
    val buttonColors = colors ?: ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    val containerColor =
        if (enabled) buttonColors.containerColor else buttonColors.disabledContainerColor
    val contentColor = if (enabled) buttonColors.contentColor else buttonColors.disabledContentColor
    val finalShape = shape ?: RoundedCornerShape(24.dp)
    val finalContentPadding = contentPadding ?: ButtonDefaults.ContentPadding
    val tonalElevation = if (enabled && elevation != null) 2.dp else 0.dp

    Box(
        modifier = modifier.pressScaleClickable(enabled, interactionSource, onClick = onClick)
    ) {
        Surface(
            shape = finalShape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = 0.dp,
            border = border,
        ) {
            Box(
                modifier = Modifier.padding(finalContentPadding)
            ) {
                AppText(
                    text = title,
                    typography = typography,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
