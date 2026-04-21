package com.thinh.snaplet.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.chat.Message
import com.thinh.snaplet.data.model.chat.MessageType
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.to24HourTime
import kotlinx.coroutines.launch
import pressScaleClickable
import kotlin.math.sin

// ─── Design tokens ────────────────────────────────────────────────────────────

private val ChatBg = Color(0xFF0D0D0D)
private val ChatSurface = Color(0xFF1A1C1C)
private val BubbleTheirs = Color(0xFF1E2020)
private val SeparatorColor = Color(0xFF1A1C1C)
private const val BUBBLE_MAX_WIDTH_FRACTION = 0.72f
private val BUBBLE_CORNER = 16.dp
private val BUBBLE_CORNER_SMALL = 4.dp

private enum class BubblePosition { FIRST, MIDDLE, LAST, SINGLE }

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    val coroutineScope = rememberCoroutineScope()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex =
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= (totalItems * 0.8f).toInt()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    // derivedStateOf makes messageCount a Compose State so snapshotFlow can track it.
    // This is key: snapshotFlow reads count + scroll position in the same snapshot frame,
    val messageCountState = remember { derivedStateOf { uiState.messages.size } }
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
            isOnline = true, // TODO: wire from uiState / socket presence event
            onNavigateBack = onNavigateBack,
            onMore = { /* TODO */ },
        )

        HorizontalDivider(color = SeparatorColor, thickness = 1.dp)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BaseText(
                            text = uiState.error ?: "",
                            color = Color.White.copy(alpha = 0.6f),
                            typography = Typography.bodyMedium,
                        )
                        OutlinedButton(onClick = viewModel::loadMessages) {
                            BaseText(
                                text = stringResource(R.string.retry),
                                color = Color.White,
                                typography = Typography.labelLarge,
                            )
                        }
                    }
                }

                else -> {
                    val lastSentByMeId = remember(uiState.messages, uiState.currentUserId) {
                        uiState.messages.firstOrNull { it.senderId == uiState.currentUserId }?.id
                    }

                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .animateContentSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        itemsIndexed(
                            items = uiState.messages,
                            key = { _, item -> item.clientUuid },
                        ) { index, message ->
                            val isMine = message.senderId == uiState.currentUserId
                            val isPending = message.clientUuid in uiState.pendingClientUuids
                            val isReadByPartner = message.id == uiState.partnerLastReadMessageId
                            val showTick =
                                isMine && message.id == lastSentByMeId && !isReadByPartner

                            // reverseLayout=true: index+1 = older (visually above),
                            //                     index-1 = newer (visually below)
                            val prevSenderSame =
                                uiState.messages.getOrNull(index + 1)?.senderId == message.senderId
                            val nextSenderSame =
                                uiState.messages.getOrNull(index - 1)?.senderId == message.senderId
                            val position = when {
                                prevSenderSame && nextSenderSame -> BubblePosition.MIDDLE
                                prevSenderSame && !nextSenderSame -> BubblePosition.LAST
                                !prevSenderSame && nextSenderSame -> BubblePosition.FIRST
                                else -> BubblePosition.SINGLE
                            }

                            MessageBubble(
                                message = message,
                                isMine = isMine,
                                isPending = isPending,
                                showTick = showTick,
                                showReadAvatar = isReadByPartner && isMine,
                                partnerAvatarUrl = viewModel.partnerAvatarUrl,
                                partnerName = viewModel.partnerName,
                                position = position,
                            )
                        }

                        // Older-messages loading spinner at the visual top
                        if (uiState.isLoadingMore) {
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

        // Typing indicator sits between the message list and the input bar
        AnimatedVisibility(
            visible = uiState.isPartnerTyping,
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
                coroutineScope.launch { listState.scrollToItem(0) }
            },
            onAttach = { /* TODO */ },
        )
    }
}

// ─── Chat header ──────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(
    name: String,
    avatarUrl: String?,
    isOnline: Boolean,
    onNavigateBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ChatBg)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                tint = MaterialTheme.colorScheme.onBackground,
            ),
            iconSize = 28.dp,
            onClick = onNavigateBack,
            iconDecoration = IconDecoration(padding = 12.dp),
        )

        // Avatar with online presence dot
        Box(modifier = Modifier.size(42.dp)) {
            Avatar(
                avatarUrl = avatarUrl,
                firstName = name,
                size = 42.dp,
            )
            if (isOnline) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                        .border(1.5.dp, ChatBg, CircleShape)
                        .align(Alignment.BottomEnd),
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = name,
                typography = Typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BaseText(
                text = if (isOnline) "Đang hoạt động" else "Không hoạt động",
                typography = Typography.labelSmall,
                color = if (isOnline) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.4f),
            )
        }

        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.Default.MoreVert,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = onMore,
            containerColor = Color.Transparent,
            iconDecoration = IconDecoration(padding = 12.dp),
        )
        Spacer(Modifier.width(4.dp))
    }
}

// ─── Typing indicator ─────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave",
    )

    Row(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = BUBBLE_CORNER_SMALL,
                    topEnd = BUBBLE_CORNER,
                    bottomStart = BUBBLE_CORNER,
                    bottomEnd = BUBBLE_CORNER,
                )
            )
            .background(BubbleTheirs)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val bounceHeight = 4f
        val phaseOffset = 1f / 3f

        repeat(3) { index ->
            // Continuous sine wave — dot i samples the wave at its own phase offset
            val phase = (progress - index * phaseOffset) * 2f * Math.PI.toFloat()
            val sineValue = -sin(phase)            // negative so positive = up
            val offsetY = sineValue * bounceHeight

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        translationY = offsetY
                        scaleX = 1f + if (sineValue > 0f) sineValue * 0.08f else 0f
                        scaleY = scaleX
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.5f))
            )
        }
    }
}

// ─── Message bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    isPending: Boolean,
    showTick: Boolean,
    showReadAvatar: Boolean,
    partnerAvatarUrl: String?,
    partnerName: String,
    position: BubblePosition,
) {
    val screenWidthDp = LocalWindowInfo.current.containerSize.width.dp
    val cs = MaterialTheme.colorScheme

    val bubbleColor = if (isMine) cs.onBackground else BubbleTheirs
    val textColor = if (isMine) Color(0xFF0D0D0D) else Color.White
    val metaColor = if (isMine) cs.background.copy(alpha = 0.6f)
    else cs.onBackground.copy(alpha = 0.6f)

    val shape = if (isMine) mineShape(position) else theirShape(position)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = (screenWidthDp * BUBBLE_MAX_WIDTH_FRACTION))
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when {
                message.isDeleted -> {
                    BaseText(
                        text = stringResource(R.string.conversation_deleted_message),
                        color = textColor.copy(alpha = 0.5f),
                        typography = Typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                    )
                }

                message.type == MessageType.IMAGE -> {
                    BaseText(
                        text = stringResource(R.string.conversation_message_photo),
                        color = textColor,
                        typography = Typography.bodyMedium,
                    )
                }

                else -> {
                    BaseText(
                        text = message.content.orEmpty(),
                        color = textColor,
                        typography = Typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                BaseText(
                    text = message.createdAt.to24HourTime(),
                    color = metaColor,
                    typography = Typography.labelSmall,
                )

                if (isMine) {
                    val tickAlpha = if (isPending) 0.3f else 0.5f
                    when {
                        showReadAvatar -> {
                            Avatar(
                                avatarUrl = partnerAvatarUrl,
                                firstName = partnerName,
                                size = 14.dp,
                            )
                        }

                        showTick -> {
                            Icon(
                                imageVector = if (isPending) Icons.Filled.Done else Icons.Filled.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF0D0D0D).copy(alpha = tickAlpha),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun mineShape(position: BubblePosition) = when (position) {
    BubblePosition.FIRST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER_SMALL, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.MIDDLE -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER_SMALL,
        bottomEnd = BUBBLE_CORNER_SMALL, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.LAST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER_SMALL,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.SINGLE -> RoundedCornerShape(BUBBLE_CORNER)
}

private fun theirShape(position: BubblePosition) = when (position) {
    BubblePosition.FIRST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER_SMALL,
    )

    BubblePosition.MIDDLE -> RoundedCornerShape(
        topStart = BUBBLE_CORNER_SMALL, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER_SMALL,
    )

    BubblePosition.LAST -> RoundedCornerShape(
        topStart = BUBBLE_CORNER_SMALL, topEnd = BUBBLE_CORNER,
        bottomEnd = BUBBLE_CORNER, bottomStart = BUBBLE_CORNER,
    )

    BubblePosition.SINGLE -> RoundedCornerShape(BUBBLE_CORNER)
}

// ─── Chat input bar ───────────────────────────────────────────────────────────

@Composable
private fun ChatInputBar(
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
