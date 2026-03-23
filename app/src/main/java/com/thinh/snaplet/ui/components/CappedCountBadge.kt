package com.thinh.snaplet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import thenIf

@Composable
fun CappedCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color = Color.Black,
    shape: Shape = RoundedCornerShape(8.dp),
    typography: TextStyle = MaterialTheme.typography.labelLarge,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    minSize: Dp = 24.dp,
    plusGlyphSize: Dp = 8.dp,
) {
    if (count <= 0) return

    val isOverLimit = count > 9

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minSize, minHeight = minSize)
            .background(backgroundColor, shape)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.thenIf(isOverLimit, { padding(horizontal = 2.dp) })
        ) {
            BaseText(
                text = if (isOverLimit) "9" else count.toString(),
                color = contentColor,
                fontWeight = FontWeight.Bold,
                typography = typography
            )
            if (isOverLimit) {
                Spacer(Modifier.width(2.dp))
                Canvas(modifier = Modifier.size(plusGlyphSize)) {
                    val strokeWidth = 1.dp.toPx()
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawLine(
                        contentColor,
                        Offset(0f, cy),
                        Offset(size.width, cy),
                        strokeWidth,
                        StrokeCap.Round
                    )
                    drawLine(
                        contentColor,
                        Offset(cx, 0f),
                        Offset(cx, size.height),
                        strokeWidth,
                        StrokeCap.Round
                    )
                }
            }
        }
    }
}
