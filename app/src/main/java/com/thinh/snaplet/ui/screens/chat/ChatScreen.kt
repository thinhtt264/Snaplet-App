package com.thinh.snaplet.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.thinh.snaplet.R
import com.thinh.snaplet.data.local.entity.MessageStatus
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.screens.chat.components.BubblePosition
import com.thinh.snaplet.ui.screens.chat.components.ChatHeader
import com.thinh.snaplet.ui.screens.chat.components.ChatInputBar
import com.thinh.snaplet.ui.screens.chat.components.MessageBubble
import com.thinh.snaplet.ui.screens.chat.components.TypingIndicator
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.isGreaterWithFallback
import kotlinx.coroutines.launch
import java.util.Date

private val ChatBg = Color(0xFF0D0D0D)
private val SeparatorColor = Color(0xFF1A1C1C)

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.messages.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onPause() }

    val messageCountState = remember { derivedStateOf { lazyPagingItems.itemCount } }
    LaunchedEffect(listState) {
        var prevMessageCount = 0
        snapshotFlow {
            val count = messageCountState.value
            val idx = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            val viewport =
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            count to (idx == 0 && (viewport == 0 || offset <= viewport * 0.25f))
        }.collect { (count, nearBottom) ->
            if (count > prevMessageCount && nearBottom) {
                listState.scrollToItem(0)
            }
            prevMessageCount = count
            viewModel.onIsAtBottomChanged(nearBottom)
        }
    }

    val visibleMessages by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                if (info.index < lazyPagingItems.itemCount) lazyPagingItems.peek(info.index) else null
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { visibleMessages }.collect { visible ->
            viewModel.onVisibleMessagesChanged(visible)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBg)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
    ) {
        ChatHeader(
            name = viewModel.partnerName,
            avatarUrl = viewModel.partnerAvatarUrl,
            isOnline = true,
            onNavigateBack = onNavigateBack,
            onMore = { /* TODO */ },
        )

        HorizontalDivider(color = SeparatorColor, thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isRefreshLoading = lazyPagingItems.loadState.refresh is LoadState.Loading
            val refreshError = lazyPagingItems.loadState.refresh as? LoadState.Error
            when {
                uiState.messageList.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                uiState.messageList.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BaseText(
                            text = uiState.messageList.error ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            typography = Typography.bodyMedium,
                        )
                        OutlinedButton(onClick = { viewModel.onSendMessage(uiState.draftMessage) }) {
                            BaseText(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                typography = Typography.labelLarge,
                            )
                        }
                    }
                }

                isRefreshLoading && lazyPagingItems.itemCount == 0 -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                refreshError != null && lazyPagingItems.itemCount == 0 -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BaseText(
                            text = refreshError.error.localizedMessage
                                ?: stringResource(R.string.error),
                            color = Color.White.copy(alpha = 0.6f),
                            typography = Typography.bodyMedium,
                        )
                        OutlinedButton(onClick = { lazyPagingItems.retry() }) {
                            BaseText(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                typography = Typography.labelLarge,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(
                            count = lazyPagingItems.itemCount,
                            key = { index ->
                                lazyPagingItems.peek(index)?.clientUuid ?: "item_$index"
                            },
                        ) { index ->
                            val message = lazyPagingItems[index] ?: return@items
                            val isMine = message.senderId == uiState.currentUserId
                            val isPending = message.status == MessageStatus.PENDING
                            val isError = message.status == MessageStatus.FAILED

                            val prevSenderSame = if (index + 1 < lazyPagingItems.itemCount)
                                lazyPagingItems.peek(index + 1)?.senderId == message.senderId
                            else false
                            val nextSenderSame = if (index - 1 >= 0)
                                lazyPagingItems.peek(index - 1)?.senderId == message.senderId
                            else false
                            val position = when {
                                prevSenderSame && nextSenderSame -> BubblePosition.MIDDLE
                                prevSenderSame && !nextSenderSame -> BubblePosition.LAST
                                !prevSenderSame && nextSenderSame -> BubblePosition.FIRST
                                else -> BubblePosition.SINGLE
                            }

                            val partnerReadHorizonMs = uiState.partner.readHorizonMs
                            val isPartnerSeen = isMine && isGreaterWithFallback(
                                Date(partnerReadHorizonMs ?: 0L), message.createdAt, false
                            )

                            MessageBubble(
                                message = message,
                                isMine = isMine,
                                isPending = isPending,
                                isError = isError,
                                showSeenTick = isPartnerSeen,
                                position = position,
                            )
                        }

                        if (lazyPagingItems.loadState.append is LoadState.Loading) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.partner.isTyping,
            enter = fadeIn(),
            exit = ExitTransition.None,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
            ) {
                TypingIndicator()
            }
        }

        ChatInputBar(
            value = uiState.draftMessage.orEmpty(),
            onValueChange = viewModel::onChangeDraftMessage,
            onSendMessage = { text ->
                viewModel.onSendMessage(text)
                coroutineScope.launch { listState.animateScrollToItem(0) }
            },
            onAttach = { /* TODO */ },
        )
    }
}
