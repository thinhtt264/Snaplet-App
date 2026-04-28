package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.emoji.EmojiEntry
import com.thinh.snaplet.data.model.emoji.EmojiLoader
import com.thinh.snaplet.data.model.emoji.EmojiTab
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.screens.home.QuickChatEmojiSlots
import pressScaleClickable

private const val EMOJI_GRID_COLUMNS = 8
private val EMOJI_GRID_HEIGHT = 300.dp
private val EMOJI_ICON_SIZE = 36.dp

@Composable
fun QuickChatBar(
    modifier: Modifier = Modifier,
    messageText: String,
    quickEmojiSlots: List<String> = QuickChatEmojiSlots.mergeForDisplay(emptyList()),
    onFocusChange: (Boolean) -> Unit = {},
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onEmojiSelected: (String) -> Unit,
) {
    var showEmojiSheet by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isFocused) {
        onFocusChange(isFocused)
        if (isFocused) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isFocused) {
                MessageInput(
                    text = messageText,
                    onTextChange = onMessageChange,
                    onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage()
                            focusManager.clearFocus()
                        }
                    },
                    focusRequester = focusRequester,
                    onFocusLost = {
                        isFocused = false
                        onMessageChange("")
                    },
                    modifier = Modifier.weight(1f)
                )
                SendButton(
                    enabled = messageText.isNotBlank(),
                    onClick = {
                        onSendMessage()
                        focusManager.clearFocus()
                    }
                )
            } else {
                FakeMessageInput(
                    modifier = Modifier.weight(1f),
                    onClick = { isFocused = true }
                )
                quickEmojiSlots.forEach { emoji ->
                    EmojiButton(emoji = emoji, onClick = { onEmojiSelected(emoji) })
                }
                AddReactionButton(onClick = { showEmojiSheet = true })
            }
        }
    }

    if (showEmojiSheet) {
        EmojiPickerSheet(onEmojiPicked = { emoji ->
            onEmojiSelected(emoji)
            showEmojiSheet = false
        }, onDismiss = { showEmojiSheet = false })
    }
}

@Composable
private fun FakeMessageInput(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        BaseText(
            text = stringResource(R.string.quick_chat_placeholder),
            typography = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp,
            color = placeholderColor
        )
    }
}

@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester,
    onFocusLost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp
    )
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    var wasFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    wasFocused = true
                } else if (wasFocused) {
                    onFocusLost()
                }
            },
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    BaseText(
                        text = stringResource(R.string.quick_chat_placeholder),
                        typography = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = placeholderColor
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun SendButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val background = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val iconTint = if (enabled) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .pressScaleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Send,
            contentDescription = "Send",
            modifier = Modifier.size(18.dp),
            tint = iconTint
        )
    }
}

@Composable
private fun EmojiButton(
    emoji: String, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(EMOJI_ICON_SIZE)
            .pressScaleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BaseText(text = emoji, fontSize = 24.sp)
    }
}

@Composable
private fun AddReactionButton(
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(EMOJI_ICON_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pressScaleClickable(onClick = onClick), contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AddReaction,
            contentDescription = "Add reaction",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSheet(
    onEmojiPicked: (String) -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val grouped = remember { EmojiLoader.loadGrouped(context) }
    val tabs = remember { EmojiTab.entries }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val displayEmojis = remember(searchQuery, selectedTab) {
        if (searchQuery.isNotBlank()) {
            EmojiLoader.search(context, searchQuery)
        } else {
            grouped[tabs[selectedTab]] ?: emptyList()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (searchQuery.isBlank()) {
                EmojiTabRow(
                    tabs = tabs, selectedIndex = selectedTab, onTabSelected = { selectedTab = it })
            }

            EmojiGrid(
                emojis = displayEmojis,
                onEmojiClick = onEmojiPicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EMOJI_GRID_HEIGHT)
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp
    )

    BasicTextField(
        value = query,
        enabled = false,
        onValueChange = onQueryChange,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BaseText(
                            text = "Search emoji…",
                            typography = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    inner()
                }
            }
        })
}

@Composable
private fun EmojiTabRow(
    tabs: List<EmojiTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 4.dp,
        divider = {},
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                text = { Text(text = tab.icon, fontSize = 20.sp) })
        }
    }
}

@Composable
private fun EmojiGrid(
    emojis: List<EmojiEntry>, onEmojiClick: (String) -> Unit, modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(EMOJI_GRID_COLUMNS),
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(emojis, key = { it.hexcode ?: it.unicode }) { entry ->
            Text(
                text = entry.unicode,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clickable { onEmojiClick(entry.unicode) }
                    .padding(6.dp))
        }
    }
}
