package com.thinh.snaplet.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState

@Stable
data class QuickChatBarModel(
    val messageText: String,
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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        if (isShowActivityBar) {
            PostActivityBar(model = postActivityBar)
        } else {
            QuickChatBar(
                messageText = quickChatBar.messageText,
                onMessageChange = quickChatBar.onMessageChange,
                onSendMessage = quickChatBar.onSendMessage,
                onEmojiSelected = quickChatBar.onEmojiSelected,
            )
        }

        BottomAction(
            onGridClick = bottomAction.onGridClick,
            onCaptureClick = bottomAction.onCaptureClick,
            onMoreClick = bottomAction.onMoreClick,
            showMoreButtonLoading = bottomAction.showMoreButtonLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}