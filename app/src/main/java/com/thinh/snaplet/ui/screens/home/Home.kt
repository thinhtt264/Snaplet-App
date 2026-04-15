package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.ui.components.EmojiFloatCanvas
import com.thinh.snaplet.ui.components.EmojiFloatController
import com.thinh.snaplet.ui.components.MultiplePermissionsHandler
import com.thinh.snaplet.ui.screens.home.components.BottomActionModel
import com.thinh.snaplet.ui.screens.home.components.CameraPage
import com.thinh.snaplet.ui.screens.home.components.EmptyMediaPage
import com.thinh.snaplet.ui.screens.home.components.FriendBottomSheet
import com.thinh.snaplet.ui.screens.home.components.HomeBottomContent
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.NewPostsBanner
import com.thinh.snaplet.ui.screens.home.components.PostActivityBarModel
import com.thinh.snaplet.ui.screens.home.components.PostGridView
import com.thinh.snaplet.ui.screens.home.components.QuickChatBarModel
import com.thinh.snaplet.ui.screens.home.components.ReactionsBottomSheet
import com.thinh.snaplet.ui.screens.home.components.TopAction
import com.thinh.snaplet.ui.theme.MotionTokens
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

private const val CAMERA_PAGE_INDEX = 0

@Immutable
data class CameraActions(
    val onImageCaptureReady: (ImageCapture) -> Unit,
    val onSnapshotHandlerReady: (() -> Bitmap?) -> Unit,
    val onPickFromGallery: () -> Unit,
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

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onGalleryImagePicked(uri)
        }
    }

    val pageCount = 1 + if (uiState.posts.isEmpty()) 1 else uiState.posts.size
    val pagerState = rememberPagerState(
        initialPage = CAMERA_PAGE_INDEX, pageCount = { pageCount })

    // When the post list shrinks (feed filter, reload, …), `pageCount` can drop before
    // `currentPage` catches up, leaving the user past the last valid page.
    // `scrollToPage` snaps into 0..lastPageIndex so the pager stays aligned with data.
    LaunchedEffect(pageCount) {
        val lastPageIndex = pageCount - 1
        if (pagerState.currentPage > lastPageIndex) {
            pagerState.scrollToPage(lastPageIndex)
        }
    }

    var snapshotHandler by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    val cameraActions = remember(viewModel) {
        CameraActions(
            onImageCaptureReady = viewModel::setImageCapture,
            onSnapshotHandlerReady = { snapshotHandler = it },
            onPickFromGallery = {
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onCapturePhoto = { viewModel.onCapturePhoto(context) },
            onSwitchCamera = viewModel::onSwitchCamera,
            onCancelCapture = viewModel::onCancelCapture,
            onUploadPost = { viewModel.onUploadPost() },
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

    val homePermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Permission.Notifications)
            }
            add(Permission.Camera)
        }
    }

    MultiplePermissionsHandler(
        permissions = homePermissions,
        onPermissionsResult = { map ->
            map[Permission.Camera.manifestPermission]?.let { granted ->
                viewModel.onPermissionResult(granted)
            }
        },
    ) { requestPermissions ->

        LaunchedEffect(Unit) {
            val anyMissing = homePermissions.any { perm ->
                ContextCompat.checkSelfPermission(
                    context,
                    perm.manifestPermission,
                ) != PackageManager.PERMISSION_GRANTED
            }
            if (anyMissing) requestPermissions()
        }

        HomeStateEffects(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            snackBarHostState = snackBarHostState,
            requestPermission = requestPermissions,
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
    var friendSearchQuery by remember { mutableStateOf("") }
    var chatMessage by remember { mutableStateOf("") }
    var previousPostListViewMode by remember { mutableStateOf(uiState.postListViewMode) }

    val showGlobalBottomContent by remember(uiState.postListViewMode) {
        derivedStateOf {
            val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
            uiState.postListViewMode == PostListViewMode.PAGER && absolutePosition > 1.0f
        }
    }

    val userScrollEnabled = !uiState.cameraState.isEditMode
    val isDownloading = uiState.isDownloading

    val quickChatBar = remember(chatMessage, uiState.quickChatEmojiSlots, onEmojiReaction) {
        QuickChatBarModel(
            messageText = chatMessage,
            quickEmojiSlots = uiState.quickChatEmojiSlots,
            onMessageChange = { chatMessage = it },
            onSendMessage = {
                /* TODO: send chat message */
                chatMessage = ""
            },
            onEmojiSelected = { emoji -> onEmojiReaction(emoji) },
        )
    }

    val bottomAction = remember(
        onNavigateToCameraPage,
        onMoreClick,
        isDownloading,
        uiState.postListViewMode,
    ) {
        BottomActionModel(
            onGridClick = {
                val nextMode = if (uiState.postListViewMode == PostListViewMode.PAGER) {
                    PostListViewMode.GRID
                } else {
                    PostListViewMode.PAGER
                }
                viewModel.onViewModeToggle(nextMode)
            },
            onCaptureClick = {
                if (uiState.postListViewMode == PostListViewMode.GRID) {
                    viewModel.resetFeedFilterFromGridCapture()
                    viewModel.onViewModeToggle(PostListViewMode.PAGER)
                }
                onNavigateToCameraPage()
            },
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

    LaunchedEffect(pagerState.currentPage, uiState.postListViewMode) {
        if (uiState.postListViewMode != PostListViewMode.PAGER) return@LaunchedEffect
        onItemVisible(pagerState.currentPage - 1)
    }

    LaunchedEffect(uiState.postListViewMode, uiState.pagerInitialIndex) {
        val shouldJumpToPagerItem = previousPostListViewMode == PostListViewMode.GRID &&
                uiState.postListViewMode == PostListViewMode.PAGER

        if (shouldJumpToPagerItem) {
            val targetPage = (uiState.pagerInitialIndex + 1).coerceIn(0, pagerState.pageCount - 1)
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }

        previousPostListViewMode = uiState.postListViewMode
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val isCameraPage = uiState.postListViewMode == PostListViewMode.PAGER &&
                pagerState.currentPage == CAMERA_PAGE_INDEX

        val gridToPagerTransition: ContentTransform =
            (scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(
                    durationMillis = MotionTokens.Emphasized,
                    easing = FastOutSlowInEasing
                ),
            ) + fadeIn(tween(MotionTokens.Normal))) togetherWith
                    (scaleOut(
                        targetScale = 1.04f,
                        animationSpec = tween(MotionTokens.Normal),
                    ) + fadeOut(tween(MotionTokens.Fast)))

        val pagerToGridTransition: ContentTransform =
            (scaleIn(
                initialScale = 1.04f,
                animationSpec = tween(
                    durationMillis = MotionTokens.Emphasized,
                    easing = FastOutSlowInEasing
                ),
            ) + fadeIn(tween(MotionTokens.Normal))) togetherWith
                    (scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(MotionTokens.Normal),
                    ) + fadeOut(tween(MotionTokens.Fast)))

        AnimatedContent(
            targetState = uiState.postListViewMode,
            transitionSpec = {
                if (targetState == PostListViewMode.PAGER) gridToPagerTransition
                else pagerToGridTransition
            },
            label = "PostListViewMode",
            modifier = Modifier.fillMaxSize(),
        ) { mode ->
            when (mode) {
                PostListViewMode.PAGER -> {
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
                                else -> uiState.posts.getOrNull(page - 1)?.id ?: "page_$page"
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
                                friends = uiState.friendSheetState.friendList,
                                postAudience = uiState.postAudience,
                                onPostAudienceChange = viewModel::onPostAudienceChange,
                            )

                            else -> {
                                val post = uiState.posts.getOrNull(page - 1)
                                if (post == null) {
                                    EmptyMediaPage(
                                        onAddFriendClick = viewModel::showFriendSheet,
                                        firstName = uiState.feedFilterFirstName,
                                    )
                                } else {
                                    MediaPage(
                                        post = post,
                                        uploadStatus = uiState.uploadStatuses[post.id],
                                        showBottomContent = !showGlobalBottomContent,
                                        quickChatBar = quickChatBar,
                                        bottomAction = bottomAction,
                                        postActivityBar = postActivityBar,
                                        onRetryClick = { viewModel.retryUpload(post.id) },
                                        onDeleteClick = { viewModel.deleteFailedPost(post.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                PostListViewMode.GRID -> {
                    PostGridView(
                        posts = uiState.posts,
                        onItemClick = viewModel::onGridItemClick,
                        onLoadMore = viewModel::onGridNearEndReached,
                        canLoadMore = uiState.canLoadMore,
                        onCaptureClick = {
                            viewModel.resetFeedFilterFromGridCapture()
                            viewModel.onViewModeToggle(PostListViewMode.PAGER)
                            onNavigateToCameraPage()
                        },
                        loadMoreTriggerFromBottomRatio = GetNewsfeedUseCase.GRID_TRIGGER_FROM_BOTTOM_RATIO,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        NewPostsBanner(
            bannerMessage = uiState.bannerMessage,
            isEligiblePage = uiState.postListViewMode == PostListViewMode.PAGER &&
                    pagerState.currentPage > CAMERA_PAGE_INDEX &&
                    uiState.unreadPostsCount > 0,
            onClick = viewModel::onNewPostsBannerTapped,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
        )

        TopAction(
            hasCaptureImage = uiState.cameraState.capturedImagePath != null,
            onProfileClick = onProfileClick,
            onFriendsClick = viewModel::showFriendSheet,
            onChatClick = { /* TODO */ },
            relationshipCounts = uiState.friendSheetState.relationshipCounts,
            avatarUrl = uiState.userProfile?.avatarUrls?.forThumbnail().orEmpty(),
            isCameraPage = isCameraPage,
            myUserId = uiState.userProfile?.id,
            selectedFeedUserId = uiState.feedUserIdFilter,
            acceptedFriends = uiState.friendSheetState.friendList,
            onFeedFilterUserSelected = viewModel::onFeedFilterUserSelected,
            isFeedFilterEnabled = uiState.isFeedFilterEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        )

        if (uiState.showFriendSheet) {
            FriendBottomSheet(
                onDismiss = {
                    friendSearchQuery = ""
                    viewModel.onFriendSheetDismissed()
                },
                friendSheetState = uiState.friendSheetState,
                onShareToApp = viewModel::shareToApp,
                onShareOther = viewModel::shareOther,
                onSheetVisible = {
                    viewModel.loadShareApps()
                },
                searchQuery = friendSearchQuery,
                onSearchQueryChange = {
                    friendSearchQuery = it
                    viewModel.onFriendSearchQueryChanged(it)
                },
                onFriendRemove = viewModel::requestRemoveFriend,
                onPendingAccept = viewModel::acceptFriendRequest,
                onAddFriend = viewModel::sendFriendRequest,
                username = uiState.userProfile?.userName.orEmpty()
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
            val postIndex = pagerState.currentPage - 1
            if (postIndex in uiState.posts.indices) {
                val currentPost = uiState.posts[postIndex]
                HomeBottomContent(
                    quickChatBar = quickChatBar,
                    bottomAction = bottomAction,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    isShowActivityBar = currentPost.isOwnPost,
                    postActivityBar = postActivityBar,
                )
            }
        }

        EmojiFloatCanvas(
            controller = emojiFloatController,
            modifier = Modifier.fillMaxSize(),
        )
    }
}