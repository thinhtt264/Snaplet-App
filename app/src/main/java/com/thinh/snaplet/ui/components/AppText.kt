package com.thinh.snaplet.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.thinh.snaplet.ui.theme.AppFontFamily

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    typography: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = FontWeight.Normal,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified
) {
    val finalStyle = typography.copy(
        fontFamily = AppFontFamily,
        fontWeight = fontWeight ?: typography.fontWeight ?: FontWeight.Normal,
        fontStyle = fontStyle ?: typography.fontStyle ?: FontStyle.Normal,
        fontSize = if (fontSize != TextUnit.Unspecified) fontSize else typography.fontSize,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else typography.lineHeight,
        letterSpacing = if (letterSpacing != TextUnit.Unspecified) letterSpacing else typography.letterSpacing,
        textDecoration = textDecoration ?: typography.textDecoration
    )

    Text(
        text = text,
        modifier = modifier,
        style = finalStyle,
        color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onBackground,
        textAlign = textAlign ?: TextAlign.Start,
        maxLines = maxLines,
        overflow = overflow
    )
}

