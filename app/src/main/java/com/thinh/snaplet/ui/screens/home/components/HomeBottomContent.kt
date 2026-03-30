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

@Composable
fun HomeBottomContent(
    quickChatBar: QuickChatBarModel,
    bottomAction: BottomActionModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        QuickChatBar(
            messageText = quickChatBar.messageText,
            onMessageChange = quickChatBar.onMessageChange,
            onSendMessage = quickChatBar.onSendMessage,
            onEmojiSelected = quickChatBar.onEmojiSelected,
        )

        BottomAction(
            onGridClick = bottomAction.onGridClick,
            onCaptureClick = bottomAction.onCaptureClick,
            onMoreClick = bottomAction.onMoreClick,
            showMoreButtonLoading = bottomAction.showMoreButtonLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}