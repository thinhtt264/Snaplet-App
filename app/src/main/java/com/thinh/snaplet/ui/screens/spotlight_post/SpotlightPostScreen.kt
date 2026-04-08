package com.thinh.snaplet.ui.screens.spotlight_post

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.R
import com.thinh.snaplet.ui.components.BaseText
import com.thinh.snaplet.ui.components.EmojiFloatCanvas
import com.thinh.snaplet.ui.components.PrimaryButton
import com.thinh.snaplet.ui.screens.home.PostReactionsUiState
import com.thinh.snaplet.ui.screens.home.components.BottomActionModel
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.PostActivityBar
import com.thinh.snaplet.ui.screens.home.components.PostActivityBarModel
import com.thinh.snaplet.ui.screens.home.components.QuickChatBar
import com.thinh.snaplet.ui.screens.home.components.QuickChatBarModel
import com.thinh.snaplet.ui.screens.home.components.ReactionsBottomSheet

/** Same horizontal inset + bottom inset as [com.thinh.snaplet.ui.screens.home.components.HomeBottomContent] for the activity row only (no BottomAction). */
@Composable
private fun BoxScope.SpotlightPostActivityBar(model: PostActivityBarModel) {
    PostActivityBar(
        model = model,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .wrapContentWidth()
            .navigationBarsPadding()
            .padding(horizontal = 64.dp),
    )
}

@Composable
private fun BoxScope.SpotlightQuickChatBar(model: QuickChatBarModel) {
    QuickChatBar(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        messageText = model.messageText,
        quickEmojiSlots = model.quickEmojiSlots,
        onMessageChange = model.onMessageChange,
        onSendMessage = model.onSendMessage,
        onEmojiSelected = model.onEmojiSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotlightPostScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpotlightPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(onBack = onNavigateBack)

    val reactionsState = uiState.postReactionsState

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
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
                    .fillMaxSize()
                    .padding(padding),
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
                            SpotlightPostActivityBar(
                                model = PostActivityBarModel(
                                    state = PostReactionsUiState.Loading,
                                    onClick = viewModel::onPostActivityClick,
                                ),
                            )
                        }
                    }

                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            BaseText(text = uiState.error!!.asString(context))
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(
                                title = stringResource(R.string.retry),
                                onClick = viewModel::loadPost,
                            )
                        }
                    }

                    uiState.post != null -> {
                        val post = uiState.post!!
                        val quickChatBar = QuickChatBarModel(
                            messageText = "",
                            quickEmojiSlots = uiState.quickChatEmojiSlots,
                            onMessageChange = {},
                            onSendMessage = {},
                            onEmojiSelected = viewModel::onEmojiReaction,
                        )
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationY = -56.dp.toPx() },
                        ) {
                            MediaPage(
                                post = post,
                                uploadStatus = null,
                                showBottomContent = false,
                                quickChatBar = quickChatBar,
                                bottomAction = bottomAction,
                                postActivityBar = postActivityBar,
                            )
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
