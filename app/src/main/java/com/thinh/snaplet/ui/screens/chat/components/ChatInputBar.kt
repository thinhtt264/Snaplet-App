package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.Typography
import pressScaleClickable

private val ChatBg = Color(0xFF0D0D0D)
private val ChatSurface = Color(0xFF1A1C1C)
private val SeparatorColor = Color(0xFF1A1C1C)

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendMessage: (text: String?) -> Unit,
    onAttach: () -> Unit,
) {
    HorizontalDivider(color = SeparatorColor, thickness = 1.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ChatBg)
            .padding(all = 8.dp)
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Attach / gallery icon
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.Outlined.Image,
                tint = Color.White.copy(alpha = 0.30f),
            ),
            iconSize = 28.dp,
            onClick = onAttach,
            containerColor = Color.Transparent,
            iconDecoration = IconDecoration(padding = 6.dp),
        )

        // Input pill: text field + emoji icon
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = Typography.bodyMedium.copy(color = Color.White),
            cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(ChatSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            BaseText(
                                text = stringResource(R.string.chat_input_placeholder),
                                color = MaterialTheme.colorScheme.onSurface,
                                typography = Typography.bodyMedium,
                            )
                        }
                        innerTextField()
                    }
                    Icon(
                        imageVector = Icons.Outlined.EmojiEmotions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
        )

        val isActive = value.isNotBlank()
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else ChatSurface)
                .pressScaleClickable(
                    enabled = isActive,
                    onClick = { onSendMessage(value) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = if (isActive) MaterialTheme.colorScheme.background
                else Color.White.copy(alpha = 0.20f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}