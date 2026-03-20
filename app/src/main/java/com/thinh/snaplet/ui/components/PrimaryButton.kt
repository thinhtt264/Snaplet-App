package com.thinh.snaplet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pressScaleClickable

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors? = null,
    elevation: ButtonElevation? = null,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape? = null,
    border: BorderStroke? = null,
    typography: TextStyle = MaterialTheme.typography.titleLarge,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val buttonColors = colors ?: ButtonDefaults.buttonColors(containerColor = Color.Transparent)
    val containerColor =
        if (enabled) buttonColors.containerColor else buttonColors.disabledContainerColor
    val contentColor = if (enabled) buttonColors.contentColor else buttonColors.disabledContentColor
    val finalShape = shape ?: RoundedCornerShape(24.dp)
    val finalContentPadding = contentPadding ?: ButtonDefaults.ContentPadding
    val tonalElevation = if (enabled && elevation != null) 2.dp else 0.dp

    Surface(
        shape = finalShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = 2.dp,
        border = border,
        modifier = modifier.pressScaleClickable(
            enabled && !isLoading,
            interactionSource,
            onClick = onClick
        ),
    ) {
        Box(
            modifier = Modifier.padding(finalContentPadding),
            contentAlignment = Alignment.Center
        ) {
            val contentAlpha = if (isLoading) 0f else 1f
            if (leadingIcon != null) {
                Row(
                    modifier = Modifier.alpha(contentAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    leadingIcon()
                    Spacer(Modifier.size(4.dp))
                    BaseText(
                        text = title,
                        typography = typography,
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                BaseText(
                    modifier = Modifier.alpha(contentAlpha),
                    text = title,
                    typography = typography,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = if (titleColor != Color.Unspecified) titleColor else contentColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}