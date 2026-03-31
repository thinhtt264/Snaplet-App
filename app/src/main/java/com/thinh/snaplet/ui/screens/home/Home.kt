package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.ui.components.EmojiFloatCanvas
import com.thinh.snaplet.ui.components.EmojiFloatController
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.BottomActionModel
import com.thinh.snaplet.ui.screens.home.components.CameraPage
import com.thinh.snaplet.ui.screens.home.components.EmptyMediaPage
import com.thinh.snaplet.ui.screens.home.components.FriendBottomSheet
import com.thinh.snaplet.ui.screens.home.components.HomeBottomContent
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.NewPostsBanner
import com.thinh.snaplet.ui.screens.home.components.ReactionsBottomSheet
import com.thinh.snaplet.ui.screens.home.components.PostActivityBarModel
import com.thinh.snaplet.ui.screens.home.components.QuickChatBarModel
import com.thinh.snaplet.ui.screens.home.components.TopAction
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

private const val CAMERA_PAGE_INDEX = 0

@Immutable
data class CameraActions(
    val onImageCaptureReady: (ImageCapture) -> Unit,
    val onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    val onCapturePhoto: () -> Unit,
    val onSwitchCamera: () -> Unit,
    val onCancelCapture: () -> Unit,
    val onUploadPost: () -> Unit,
    val onCaptionChange: (String) -> Unit,
    val onRequestPermission: () -> Unit
)

@Composable
fun Home(
    onProfileClick: () -> Unit = {}, viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val pageCount = 1 + if (uiState.posts.isEmpty()) 1 else uiState.posts.size
    val pagerState = rememberPagerState(
        initialPage = CAMERA_PAGE_INDEX, pageCount = { pageCount })

    var snapshotHandler by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    val cameraActions = remember(viewModel) {
        CameraActions(
            onImageCaptureReady = viewModel::setImageCapture,
            onSnapshotHandlerReady = { snapshotHandler = it },
            onCapturePhoto = { viewModel.onCapturePhoto(context) },
            onSwitchCamera = viewModel::onSwitchCamera,
            onCancelCapture = viewModel::onCancelCapture,
            onUploadPost = viewModel::onUploadPost,
            onCaptionChange = viewModel::updateCurrentCaption,
            onRequestPermission = viewModel::onRequestCameraPermission
        )
    }

    CameraBindingEffect(
        pagerState = pagerState,
        shouldBindCamera = uiState.cameraState.shouldBindCamera,
        snapshotHandler = snapshotHandler,
        onCameraPageVisible = viewModel::onCameraPageVisible,
        onCameraPageHidden = viewModel::onCameraPageHidden,
        onSnapshotCaptured = viewModel::setPreviewSnapshot
    )

    val snackBarHostState = remember { SnackbarHostState() }

    val scrollToFirstPost: suspend () -> Unit = {
        if (pagerState.currentPage != 1) {
            pagerState.animateScrollToPage(1)
        }
    }

    PermissionHandler(
        permission = Permission.Camera, onPermissionResult = viewModel::onPermissionResult
    ) { requestPermission ->

        HomeStateEffects(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            snackBarHostState = snackBarHostState,
            requestPermission = requestPermission,
            onScrollToFirstPost = scrollToFirstPost,
        )

        HomeScreen(
            pagerState = pagerState,
            uiState = uiState,
            viewModel = viewModel,
            cameraActions = cameraActions,
            emojiFloatController = viewModel.emojiFloatController,
            onNavigateToCameraPage = {
                scope.launch { pagerState.animateScrollToPage(CAMERA_PAGE_INDEX) }
            },
            onScrollToFirstPost = {
                scope.launch { scrollToFirstPost() }
            },
            onItemVisible = viewModel::onItemVisible,
            onMoreClick = viewModel::onShowMoreOptions,
            onProfileClick = onProfileClick,
            onEmojiReaction = viewModel::onEmojiReaction,
        )

        SnackbarHost(hostState = snackBarHostState)
    }
}

@Composable
private fun CameraBindingEffect(
    pagerState: PagerState,
    shouldBindCamera: Boolean,
    snapshotHandler: (() -> Bitmap?)?,
    onCameraPageVisible: () -> Unit,
    onCameraPageHidden: () -> Unit,
    onSnapshotCaptured: (Bitmap) -> Unit
) {
    val isOnCameraPage = pagerState.currentPage == CAMERA_PAGE_INDEX

    LaunchedEffect(isOnCameraPage, pagerState.isScrollInProgress) {
        when {
            isOnCameraPage && !pagerState.isScrollInProgress -> {
                onCameraPageVisible()
            }

            !isOnCameraPage && shouldBindCamera -> {
                snapshotHandler?.invoke()?.let(onSnapshotCaptured)
                onCameraPageHidden()
            }
        }
    }

    val currentShouldBindCamera by rememberUpdatedState(shouldBindCamera)
    val currentSnapshotHandler by rememberUpdatedState(snapshotHandler)
    val currentOnSnapshotCaptured by rememberUpdatedState(onSnapshotCaptured)
    val currentOnCameraPageHidden by rememberUpdatedState(onCameraPageHidden)
    val currentOnCameraPageVisible by rememberUpdatedState(onCameraPageVisible)
    val currentIsOnCameraPage by rememberUpdatedState(isOnCameraPage)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (currentShouldBindCamera) {
                        currentSnapshotHandler?.invoke()?.let(currentOnSnapshotCaptured)
                        currentOnCameraPageHidden()
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (currentIsOnCameraPage) {
                        if (currentShouldBindCamera) {
                            // Force a rebind by toggling the camera binding state
                            currentOnCameraPageHidden()
                        }
                        currentOnCameraPageVisible()
                    }
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun HomeStateEffects(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    context: Context,
    snackBarHostState: SnackbarHostState,
    requestPermission: () -> Unit,
    onScrollToFirstPost: suspend () -> Unit
) {
    uiState.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            snackBarHostState.showSnackbar(
                message = message.asString(context),
                duration = SnackbarDuration.Short,
            )
            viewModel.onSnackbarDismissed()
        }
    }

    uiState.pendingPermission?.let {
        LaunchedEffect(it) {
            requestPermission()
            viewModel.onPermissionRequestHandled()
        }
    }

    if (uiState.shouldScrollToFirstPost) {
        LaunchedEffect(true) {
            onScrollToFirstPost()
            viewModel.onScrollToFirstPostHandled()
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun HomeScreen(
    pagerState: PagerState,
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    cameraActions: CameraActions,
    emojiFloatController: EmojiFloatController,
    onNavigateToCameraPage: () -> Unit,
    onScrollToFirstPost: () -> Unit,
    onItemVisible: (currentIndex: Int) -> Unit,
    onMoreClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onEmojiReaction: (String) -> Unit = {},
) {
    var showFriendSheet by remember { mutableStateOf(false) }
    var friendSearchQuery by remember { mutableStateOf("") }
    var chatMessage by remember { mutableStateOf("") }

    val showGlobalBottomContent by remember {
        derivedStateOf {
            val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
            absolutePosition > 1.0f
        }
    }

    val userScrollEnabled = !uiState.cameraState.isEditMode
    val isDownloading = uiState.isDownloading

    val quickChatBar = remember(chatMessage, onEmojiReaction) {
        QuickChatBarModel(
            messageText = chatMessage,
            onMessageChange = { chatMessage = it },
            onSendMessage = {
                /* TODO: send chat message */
                chatMessage = ""
            },
            onEmojiSelected = { emoji -> onEmojiReaction(emoji) },
        )
    }

    val bottomAction = remember(onNavigateToCameraPage, onMoreClick, isDownloading) {
        BottomActionModel(
            onGridClick = { /* TODO */ },
            onCaptureClick = onNavigateToCameraPage,
            onMoreClick = onMoreClick,
            showMoreButtonLoading = isDownloading,
        )
    }

    val postActivityBar = remember(uiState.postReactionsState) {
        PostActivityBarModel(
            state = uiState.postReactionsState,
            onClick = { viewModel.onPostActivityClick() },
        )
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        val postIndex = currentPage - 1
        onItemVisible(postIndex)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = userScrollEnabled,
            horizontalAlignment = Alignment.CenterHorizontally,
            beyondViewportPageCount = 1,
            key = { page ->
                when {
                    page == CAMERA_PAGE_INDEX -> "camera"
                    uiState.posts.isEmpty() -> "empty_media"
                    else -> uiState.posts[page - 1].id
                }
            }) { page ->
            when (page) {
                CAMERA_PAGE_INDEX -> CameraPage(
                    onDownloadImage = viewModel::downloadCaptureImage,
                    cameraState = uiState.cameraState,
                    currentCaption = uiState.currentCaption,
                    isUploading = uiState.uploadStatuses.values.any { it is UploadStatus.Uploading },
                    cameraActions = cameraActions,
                    unreadPostsCount = uiState.unreadPostsCount,
                    onHistoryClick = onScrollToFirstPost,
                )

                else -> {
                    if (uiState.posts.isEmpty()) {
                        EmptyMediaPage(onAddFriendClick = { showFriendSheet = true })
                    } else {
                        val post = uiState.posts[page - 1]
                        MediaPage(
                            post = post,
                            uploadStatus = uiState.uploadStatuses[post.id],
                            showBottomContent = !showGlobalBottomContent,
                            quickChatBar = quickChatBar,
                            bottomAction = bottomAction,
                            postActivityBar = postActivityBar,
                            onRetryClick = { viewModel.retryUpload(post.id) },
                            onDeleteClick = { viewModel.deleteFailedPost(post.id) })
                    }
                }
            }
        }

        NewPostsBanner(
            bannerMessage = uiState.bannerMessage,
            isEligiblePage = pagerState.currentPage > CAMERA_PAGE_INDEX && uiState.unreadPostsCount > 0,
            onClick = viewModel::onNewPostsBannerTapped,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
        )

        TopAction(
            hasCaptureImage = uiState.cameraState.capturedImagePath != null,
            onProfileClick = onProfileClick,
            onFriendsClick = { showFriendSheet = true },
            onChatClick = { /* TODO */ },
            relationshipCounts = uiState.friendSheetState.relationshipCounts,
            avatarUrl = uiState.profileAvatarUrl.orEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        )

        if (showFriendSheet) {
            FriendBottomSheet(
                onDismiss = {
                    showFriendSheet = false
                    friendSearchQuery = ""
                    viewModel.onFriendSheetDismissed()
                },
                friendSheetState = uiState.friendSheetState,
                onShareToApp = viewModel::shareToApp,
                onShareOther = viewModel::shareOther,
                onSheetVisible = {
                    viewModel.loadShareApps()
                    viewModel.loadMyFriendList()
                },
                searchQuery = friendSearchQuery,
                onSearchQueryChange = {
                    friendSearchQuery = it
                    viewModel.onFriendSearchQueryChanged(it)
                },
                onFriendRemove = viewModel::requestRemoveFriend,
                onPendingAccept = viewModel::acceptFriendRequest,
                onAddFriend = viewModel::sendFriendRequest
            )
        }

        val reactionsState = uiState.postReactionsState
        if (uiState.showReactionsSheet && reactionsState is PostReactionsUiState.Result) {
            ReactionsBottomSheet(
                reactions = reactionsState.reactions,
                onDismiss = viewModel::onReactionsSheetDismissed,
            )
        }

        if (showGlobalBottomContent) {
            val currentPost = uiState.posts[pagerState.currentPage - 1]
            HomeBottomContent(
                quickChatBar = quickChatBar,
                bottomAction = bottomAction,
                modifier = Modifier.align(Alignment.BottomCenter),
                isShowActivityBar = currentPost.isOwnPost,
            )
        }

        EmojiFloatCanvas(
            controller = emojiFloatController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}