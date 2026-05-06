package com.thinh.snaplet.ui.screens.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.model.emoji.EmojiEntry
import com.thinh.snaplet.data.model.emoji.EmojiLoader
import com.thinh.snaplet.data.model.emoji.EmojiTab
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.EmojiTabGrid
import pressScaleClickable

private val ActionBackground = RoundedCornerShape(14.dp)

@Composable
fun BoxScope.MessageInspectOverlay(
    message: MessageEntity,
    isMine: Boolean,
    panelTop: Dp,
    bubbleCenterXDp: Dp,
    chatAreaWidthDp: Dp,
    maxHeight: Dp = Dp.Unspecified,
    isFlipped: Boolean = false,
    recentEmojis: List<String>,
    onEmojiClick: (String) -> Unit,
    onCopy: (MessageEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var showEmojiSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val groupedEmojis = remember { EmojiLoader.loadGrouped(context) }

    fun dismissAll() {
        showEmojiSheet = false
        onDismiss()
    }

    val heightModifier =
        if (maxHeight != Dp.Unspecified) Modifier.heightIn(max = maxHeight) else Modifier

    Column(
        modifier = Modifier
            .offset(y = panelTop)
            .then(
                if (isMine) {
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = chatAreaWidthDp - bubbleCenterXDp)
                } else {
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = bubbleCenterXDp)
                }
            )
            .wrapContentWidth()
            .then(heightModifier)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!isFlipped) {
            RecentEmojiBar(
                recentEmojis = recentEmojis,
                onEmojiClick = { onEmojiClick(it); dismissAll() },
                onPlusClick = { showEmojiSheet = true },
            )
            ActionMenu(
                isMine = isMine,
                onCopy = { onCopy(message); dismissAll() },
            )
        } else {
            ActionMenu(
                isMine = isMine,
                onCopy = { onCopy(message); dismissAll() },
            )
            RecentEmojiBar(
                recentEmojis = recentEmojis,
                onEmojiClick = { onEmojiClick(it); dismissAll() },
                onPlusClick = { showEmojiSheet = true },
            )
        }
    }
    if (showEmojiSheet) {
        EmojiPickerBottomSheet(
            groupedEmojis = groupedEmojis,
            onDismiss = { showEmojiSheet = false },
            onEmojiClick = { onEmojiClick(it); dismissAll() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerBottomSheet(
    groupedEmojis: Map<EmojiTab, List<EmojiEntry>>,
    onDismiss: () -> Unit,
    onEmojiClick: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        EmojiTabGrid(
            groupedEmojis = groupedEmojis,
            columns = 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(bottom = 16.dp),
            onEmojiClick = { onEmojiClick(it.unicode) },
        )
    }
}

@Composable
private fun RecentEmojiBar(
    recentEmojis: List<String>,
    onEmojiClick: (String) -> Unit,
    onPlusClick: () -> Unit,
) {
    val display = recentEmojis.take(4)
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        display.forEach { emoji ->
            BaseText(
                text = emoji,
                typography = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable { onEmojiClick(emoji) },
            )
        }

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .pressScaleClickable(onClick = onPlusClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AddReaction,
                contentDescription = "Add reaction",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ActionMenu(
    isMine: Boolean,
    onCopy: () -> Unit,
) {
    val actionTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    Column(
        modifier = Modifier
            .widthIn(min = 180.dp, max = 230.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = ActionBackground,
            )
            .padding(vertical = 6.dp),
    ) {
        ActionRow(
            text = stringResource(R.string.chat_message_action_reply),
            color = actionTextColor,
            imageVector = Icons.AutoMirrored.Outlined.Reply,
            onClick = { /* TODO */ },
        )
        ActionRow(
            text = stringResource(R.string.chat_message_action_copy),
            color = actionTextColor,
            imageVector = Icons.Outlined.ContentCopy,
            onClick = onCopy,
        )
        if (isMine) {
            ActionRow(
                text = stringResource(R.string.chat_message_action_recall),
                color = MaterialTheme.colorScheme.error,
                imageVector = Icons.Outlined.DeleteOutline,
                onClick = { /* TODO */ },
            )
        }
    }
}

@Composable
private fun ActionRow(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    imageVector: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(10.dp)
            .pressScaleClickable(onClick = onClick),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = color,
        )
        BaseText(
            text = text,
            color = color,
            typography = MaterialTheme.typography.bodyMedium,
        )
    }
}