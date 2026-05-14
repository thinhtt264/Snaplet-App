package com.thinh.snaplet.ui.screens.spotlight_post

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LockPerson
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.EmojiFloatCanvas
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState
import com.thinh.snaplet.ui.screens.home.components.BottomActionModel
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.PostActivityBar
import com.thinh.snaplet.ui.screens.home.components.PostActivityBarModel
import com.thinh.snaplet.ui.screens.home.components.QuickChatBar
import com.thinh.snaplet.ui.screens.home.components.QuickChatBarModel
import com.thinh.snaplet.ui.screens.home.components.ReactionsBottomSheet

private data class SpotlightStatusContent(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val primaryActionRes: Int,
    val primaryAction: () -> Unit,
    val secondaryActionRes: Int? = null,
    val secondaryAction: (() -> Unit)? = null,
)

@Composable
private fun SpotlightPostActivityBar(model: PostActivityBarModel) {
    PostActivityBar(
        model = model,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth()
            .padding(horizontal = 64.dp),
    )
}

@Composable
private fun SpotlightQuickChatBar(model: QuickChatBarModel) {
    QuickChatBar(
        modifier = Modifier.padding(horizontal = 32.dp),
        messageText = model.messageText,
        quickEmojiSlots = model.quickEmojiSlots,
        onMessageChange = model.onMessageChange,
        onSendMessage = model.onSendMessage,
        onEmojiSelected = model.onEmojiSelected,
    )
}

@Composable
private fun SpotlightStatusContent(
    content: SpotlightStatusContent,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.padding(bottom = 20.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = content.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        BaseText(
            text = stringResource(content.titleRes),
            typography = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(8.dp))
        BaseText(
            text = stringResource(content.descriptionRes),
            typography = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(72.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = content.primaryAction,
            contentPadding = PaddingValues(vertical = 14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
        ) {
            BaseText(
                text = stringResource(content.primaryActionRes),
                typography = MaterialTheme.typography.titleMedium,
            )
        }

        if (content.secondaryAction != null && content.secondaryActionRes != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = content.secondaryAction,
                contentPadding = PaddingValues(vertical = 14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
            ) {
                BaseText(
                    text = stringResource(content.secondaryActionRes),
                    typography = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotlightPostScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: SpotlightPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onNavigateBack)

    val reactionsState = uiState.postReactionsState
    val statusContent = when (uiState.status) {
        SpotlightPostStatus.Forbidden -> SpotlightStatusContent(
            icon = Icons.Outlined.LockPerson,
            titleRes = R.string.spotlight_post_private_title,
            descriptionRes = R.string.spotlight_post_private_message,
            primaryActionRes = R.string.spotlight_send_friend_request,
            primaryAction = viewModel::onShareFriendInviteClick,
            secondaryActionRes = R.string.spotlight_back_home,
            secondaryAction = onNavigateHome,
        )

        SpotlightPostStatus.NotFound -> SpotlightStatusContent(
            icon = Icons.Outlined.SearchOff,
            titleRes = R.string.spotlight_post_not_found_title,
            descriptionRes = R.string.spotlight_post_not_found_message,
            primaryActionRes = R.string.spotlight_back_home,
            primaryAction = onNavigateHome,
        )

        SpotlightPostStatus.LoadFailed -> SpotlightStatusContent(
            icon = Icons.Outlined.CloudOff,
            titleRes = R.string.spotlight_post_load_failed_title,
            descriptionRes = R.string.spotlight_post_load_failed_message,
            primaryActionRes = R.string.retry,
            primaryAction = viewModel::loadPost,
            secondaryActionRes = R.string.spotlight_back_home,
            secondaryAction = onNavigateHome,
        )

        null -> null
    }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                CenterAlignedTopAppBar(
                    expandedHeight = 72.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    title = {
                        BaseText(
                            text = stringResource(R.string.spotlight_post_title),
                            typography = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.profile_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
            ) {
                when {
                    uiState.isLoading && uiState.post == null -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    statusContent != null -> {
                        SpotlightStatusContent(content = statusContent)
                    }

                    uiState.post != null -> {
                        val post = uiState.post!!
                        var chatMessage by remember(post.id) { mutableStateOf("") }
                        val quickChatBar = remember(
                            chatMessage,
                            uiState.quickChatEmojiSlots,
                            post.id,
                            viewModel,
                        ) {
                            QuickChatBarModel(
                                messageText = chatMessage,
                                quickEmojiSlots = uiState.quickChatEmojiSlots,
                                onMessageChange = { chatMessage = it },
                                onSendMessage = {
                                    viewModel.sendQuickChatFromPost(chatMessage)
                                    chatMessage = ""
                                },
                                onEmojiSelected = viewModel::onEmojiReaction,
                            )
                        }
                        val bottomAction = BottomActionModel(
                            onGridClick = {},
                            onCaptureClick = {},
                            onMoreClick = {},
                            showMoreButtonLoading = false,
                        )
                        val postActivityBar = PostActivityBarModel(
                            state = uiState.postReactionsState,
                            onClick = viewModel::onPostActivityClick,
                        )

                        val scrollState = rememberScrollState()

                        LaunchedEffect(imeBottom) {
                            if (imeBottom > 0) {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .imePadding()
                                .navigationBarsPadding()
                                .verticalScroll(scrollState)
                        ) {
                            Spacer(Modifier.height(60.dp))

                            MediaPage(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                post = post,
                                uploadStatus = null,
                                showBottomContent = false,
                                quickChatBar = quickChatBar,
                                bottomAction = bottomAction,
                                postActivityBar = postActivityBar,
                                onRetryClick = {},
                                onDeleteClick = {})

                            Spacer(Modifier.height(36.dp))
                            if (post.isOwnPost) {
                                SpotlightPostActivityBar(model = postActivityBar)
                            } else {
                                SpotlightQuickChatBar(model = quickChatBar)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showReactionsSheet && reactionsState is PostReactionsUiState.Result) {
            ReactionsBottomSheet(
                reactions = reactionsState.reactions,
                onDismiss = viewModel::onReactionsSheetDismissed,
            )
        }

        EmojiFloatCanvas(
            controller = viewModel.emojiFloatController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
