package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState

@Stable
data class QuickChatBarModel(
    val messageText: String,
    val quickEmojiSlots: List<String>,
    val onMessageChange: (String) -> Unit,
    val onSendMessage: () -> Unit,
    val onEmojiSelected: (String) -> Unit,
)

@Stable
data class BottomActionModel(
    val onGridClick: () -> Unit,
    val onCaptureClick: () -> Unit,
    val onMoreClick: () -> Unit,
    val showMoreButtonLoading: Boolean,
)

@Stable
data class PostActivityBarModel(
    val state: PostReactionsUiState = PostReactionsUiState.Loading,
    val onClick: () -> Unit = {},
)

@Composable
fun HomeBottomContent(
    modifier: Modifier = Modifier,
    quickChatBar: QuickChatBarModel,
    bottomAction: BottomActionModel,
    isShowActivityBar: Boolean,
    postActivityBar: PostActivityBarModel = PostActivityBarModel(),
    onQuickChatFocusChange: (Boolean) -> Unit = {},
) {
    var isInputFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                start = 32.dp,
                end = 32.dp,
                top = 0.dp,
                bottom = if (isInputFocused) 0.dp else 24.dp
            )
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (isShowActivityBar) {
            PostActivityBar(model = postActivityBar)
        } else {
            QuickChatBar(
                messageText = quickChatBar.messageText,
                quickEmojiSlots = quickChatBar.quickEmojiSlots,
                onFocusChange = { focused ->
                    isInputFocused = focused
                    onQuickChatFocusChange(focused)
                },
                onMessageChange = quickChatBar.onMessageChange,
                onSendMessage = quickChatBar.onSendMessage,
                onEmojiSelected = quickChatBar.onEmojiSelected,
            )
        }

        if (!isInputFocused) {
            BottomAction(
                onGridClick = bottomAction.onGridClick,
                onCaptureClick = bottomAction.onCaptureClick,
                onMoreClick = bottomAction.onMoreClick,
                showMoreButtonLoading = bottomAction.showMoreButtonLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
