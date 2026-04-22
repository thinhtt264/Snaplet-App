package com.thinh.snaplet.ui.screens.conversation_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.chat.Conversation
import com.thinh.snaplet.data.model.chat.LastMessage
import com.thinh.snaplet.navigation.ChatConversation
import com.thinh.snaplet.ui.components.AppIconButton
import com.thinh.snaplet.ui.components.Avatar
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.IconDecoration
import com.thinh.snaplet.ui.components.IconSpec
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.theme.Typography
import com.thinh.snaplet.utils.toLocalTimeAgo
import pressScaleClickable

@Composable
fun ConversationListScreen(
    onNavigateBack: () -> Unit,
    onConversationClick: (Conversation) -> Unit = {},
    onNavigateToNewChat: (ChatConversation) -> Unit = {},
    onAddFriendClick: () -> Unit = {},
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showNewMessageSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        ConversationHeader(
            onNavigateBack = onNavigateBack,
            onOpenNewMessage = {
                showNewMessageSheet = true
                viewModel.loadFriendList()
            },
        )
        ConversationSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
        ConversationContent(
            uiState = uiState,
            searchQuery = searchQuery,
            onConversationClick = onConversationClick,
            onRetry = viewModel::loadConversations,
            onLoadMore = viewModel::loadMore,
        )
    }

    if (showNewMessageSheet) {
        NewMessageBottomSheet(
            uiState = uiState,
            onDismiss = { showNewMessageSheet = false },
            onFriendClick = { friend ->
                showNewMessageSheet = false
                onNavigateToNewChat(
                    ChatConversation(
                        recipientId = friend.userId,
                        partnerName = friend.displayName,
                        partnerAvatarUrl = friend.avatarUrls.forThumbnail(),
                    )
                )
            },
            onAddFriendClick = {
                showNewMessageSheet = false
                onAddFriendClick()
            },
        )
    }
}

// ─── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun ConversationHeader(
    onNavigateBack: () -> Unit,
    onOpenNewMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = onNavigateBack,
            containerColor = Color.Transparent,
            iconDecoration = IconDecoration(padding = 12.dp),
        )
        BaseText(
            text = stringResource(R.string.conversation_list_title),
            typography = Typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.Default.MoreVert,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = {},
            iconDecoration = IconDecoration(padding = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        AppIconButton(
            icon = IconSpec.Vector(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                tint = Color.White,
            ),
            iconSize = 28.dp,
            onClick = onOpenNewMessage,
            iconDecoration = IconDecoration(padding = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
    }
}

// ─── Search bar ──────────────────────────────────────────────────────────────

@Composable
private fun ConversationSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        cursorBrush = SolidColor(cs.primary),
        textStyle = Typography.bodyMedium.copy(cs.onBackground),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = cs.onSurface,
                    modifier = Modifier.size(24.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BaseText(
                            text = stringResource(R.string.conversation_search_hint),
                            typography = Typography.bodyMedium,
                            color = cs.onSurface,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

// ─── Content ─────────────────────────────────────────────────────────────────

@Composable
private fun ConversationContent(
    uiState: ConversationListUiState,
    searchQuery: String,
    onConversationClick: (Conversation) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                        text = stringResource(R.string.conversation_list_load_failed),
                        color = MaterialTheme.colorScheme.outline,
                        typography = Typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onRetry) {
                        BaseText(
                            text = stringResource(R.string.retry),
                            color = MaterialTheme.colorScheme.onSurface,
                            typography = Typography.labelLarge,
                        )
                    }
                }
            }

            uiState.conversations.isEmpty() -> {
                BaseText(
                    text = stringResource(R.string.conversation_list_empty),
                    color = MaterialTheme.colorScheme.outline,
                    typography = Typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                val displayList =
                    if (searchQuery.isBlank()) uiState.conversations else uiState.conversations

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(displayList, key = { it.conversation.id }) { item ->
                        ConversationCard(
                            conversation = item.conversation,
                            isUnread = item.isUnread,
                            onClick = { onConversationClick(item.conversation) },
                        )
                        HorizontalDivider(
                            color = Color(0xFF161818),
                            thickness = 1.dp,
                            modifier = Modifier.padding(start = 76.dp),
                        )
                    }

                    if (uiState.canLoadMore) {
                        item(key = "load_more") {
                            LaunchedEffect(Unit) { onLoadMore() }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Conversation card ────────────────────────────────────────────────────────

@Composable
private fun ConversationCard(
    conversation: Conversation,
    isUnread: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    val nameColor = if (isUnread) cs.onBackground else cs.onSurface
    val previewColor = if (isUnread) cs.onBackground else cs.onSurface
    val metaColor = if (isUnread) cs.primary else cs.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarUrl = conversation.partner.avatarUrl,
            firstName = conversation.partner.displayName,
            size = 48.dp,
            modifier = Modifier.alpha(if (isUnread) 1f else 0.7f),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = conversation.partner.displayName,
                color = nameColor,
                typography = Typography.bodyMedium,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (conversation.lastMessage != null) {
                BaseText(
                    text = lastMessagePreview(conversation.lastMessage),
                    color = previewColor,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    typography = Typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            conversation.lastMessage?.createdAt?.let { time ->
                BaseText(
                    text = time.toLocalTimeAgo(),
                    color = metaColor,
                    typography = Typography.labelSmall,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(cs.primary)
                        .align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun lastMessagePreview(msg: LastMessage): String {
    return when {
        msg.isDeleted -> stringResource(R.string.conversation_deleted_message)
        msg.type == "image" -> stringResource(R.string.conversation_message_photo)
        else -> msg.content.orEmpty()
    }
}

// ─── New message bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewMessageBottomSheet(
    uiState: ConversationListUiState,
    onDismiss: () -> Unit,
    onFriendClick: (RelationshipWithUser) -> Unit,
    onAddFriendClick: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredFriends = remember(uiState.friendList, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.friendList
        } else {
            val query = searchQuery.trim().lowercase()
            uiState.friendList.filter {
                it.displayName.lowercase().contains(query) || it.username.lowercase()
                    .contains(query)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            BaseText(
                text = stringResource(R.string.new_message_title),
                typography = Typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            NewMessageSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )

            when {
                uiState.isFriendListLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.friendListError != null -> {
                    BaseText(
                        text = stringResource(R.string.new_message_friends_load_failed),
                        color = MaterialTheme.colorScheme.outline,
                        typography = Typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }

                filteredFriends.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterVertically
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        BaseText(
                            text = stringResource(R.string.new_message_friends_empty),
                            color = MaterialTheme.colorScheme.onBackground,
                            typography = Typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        BaseText(
                            text = stringResource(R.string.new_message_friends_empty_subtitle),
                            color = MaterialTheme.colorScheme.outline,
                            typography = Typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        PrimaryButton(
                            onClick = onAddFriendClick,
                            title = stringResource(R.string.new_message_find_friends),
                            titleColor = Color.Black,
                            typography = Typography.titleSmall,
                            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.6f)
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.PersonSearch,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }

                else -> {
                    BaseText(
                        text = stringResource(R.string.friends),
                        typography = Typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(filteredFriends, key = { it.userId }) { friend ->
                            FriendRow(
                                friend = friend,
                                onClick = { onFriendClick(friend) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewMessageSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        cursorBrush = SolidColor(cs.primary),
        textStyle = Typography.bodyMedium.copy(cs.onBackground),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = cs.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        BaseText(
                            text = stringResource(R.string.new_message_search_hint),
                            typography = Typography.bodyMedium,
                            color = cs.onSurface,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun FriendRow(
    friend: RelationshipWithUser,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScaleClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            avatarUrl = friend.avatarUrls.forThumbnail(),
            firstName = friend.firstName.ifBlank { friend.username },
            size = 48.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            BaseText(
                text = friend.displayName,
                color = MaterialTheme.colorScheme.onBackground,
                typography = Typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BaseText(
                text = "@${friend.username}",
                color = MaterialTheme.colorScheme.onSurface,
                typography = Typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}
