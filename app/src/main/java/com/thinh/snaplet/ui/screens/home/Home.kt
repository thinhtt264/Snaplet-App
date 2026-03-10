package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.BottomAction
import com.thinh.snaplet.ui.screens.home.components.CameraPage
import com.thinh.snaplet.ui.screens.home.components.EmptyMediaPage
import com.thinh.snaplet.ui.screens.home.components.FriendBottomSheet
import com.thinh.snaplet.ui.screens.home.components.MediaPage
import com.thinh.snaplet.ui.screens.home.components.TopAction
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
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
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

    PermissionHandler(
        permission = Permission.Camera, onPermissionResult = viewModel::onPermissionResult
    ) { requestPermission ->

        HomeStateEffects(
            uiState = uiState,
            viewModel = viewModel,
            context = context,
            snackBarHostState = snackBarHostState,
            requestPermission = requestPermission,
            onScrollToFirstPost = {
                if (pagerState.currentPage == CAMERA_PAGE_INDEX) pagerState.animateScrollToPage(
                    1
                )
            }
        )

        HomeScreen(
            pagerState = pagerState,
            uiState = uiState,
            viewModel = viewModel,
            cameraActions = cameraActions,
            onNavigateToCameraPage = {
                scope.launch { pagerState.animateScrollToPage(CAMERA_PAGE_INDEX) }
            },
            onItemVisible = viewModel::onItemVisible,
            onMoreClick = viewModel::onShowMoreOptions,
            onProfileClick = onProfileClick
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

@Composable
private fun HomeScreen(
    pagerState: PagerState,
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    cameraActions: CameraActions,
    onNavigateToCameraPage: () -> Unit,
    onItemVisible: (currentIndex: Int) -> Unit,
    onMoreClick: () -> Unit,
    onProfileClick: () -> Unit = {},
) {
    var showFriendSheet by remember { mutableStateOf(false) }
    var friendSearchQuery by remember { mutableStateOf("") }

    val showGlobalBottomAction by remember {
        derivedStateOf {
            val absolutePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
            absolutePosition > 1.0f
        }
    }

    val userScrollEnabled = !uiState.cameraState.isEditMode
    val isDownloading = uiState.isDownloading

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage
        if (currentPage > CAMERA_PAGE_INDEX) {
            val postIndex = currentPage - 1
            onItemVisible(postIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomePager(
            pagerState = pagerState,
            posts = uiState.posts,
            uploadStatuses = uiState.uploadStatuses,
            showMoreButtonLoading = isDownloading,
            cameraState = uiState.cameraState,
            currentCaption = uiState.currentCaption,
            isUploading = uiState.uploadStatuses.values.any { it is UploadStatus.Uploading },
            showLocalBottomAction = !showGlobalBottomAction,
            userScrollEnabled = userScrollEnabled,
            cameraActions = cameraActions,
            onNavigateToCameraPage = onNavigateToCameraPage,
            onMoreClick = onMoreClick,
            onShowFriendSheet = { showFriendSheet = true },
            onRetryUpload = viewModel::retryUpload,
            onDeleteFailedPost = viewModel::deleteFailedPost
        )

        TopAction(
            onProfileClick = onProfileClick,
            onFriendsClick = { showFriendSheet = true },
            onChatClick = { /* TODO */ },
            friendsCount = uiState.friendSheetState.friendsCount,
            avatarUrl = uiState.profileAvatarUrl.orEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(all = 16.dp)
        )

        if (showFriendSheet) {
            FriendBottomSheet(
                onDismiss = { showFriendSheet = false },
                friendSheetState = uiState.friendSheetState,
                onShareToApp = viewModel::shareToApp,
                onShareOther = viewModel::shareOther,
                onSheetVisible = {
                    viewModel.loadShareApps()
                    viewModel.loadMyFriendList()
                },
                searchQuery = friendSearchQuery,
                onSearchQueryChange = { friendSearchQuery = it },
                onFriendRemove = viewModel::removeFriend,
                onPendingAccept = viewModel::acceptFriendRequest
            )
        }

        if (showGlobalBottomAction) {
            BottomAction(
                onGridClick = { /* TODO */ },
                onCaptureClick = onNavigateToCameraPage,
                onMoreClick = onMoreClick,
                showMoreButtonLoading = isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun HomePager(
    pagerState: PagerState,
    posts: List<Post>,
    uploadStatuses: Map<String, UploadStatus>,
    showMoreButtonLoading: Boolean,
    cameraState: CameraState,
    currentCaption: String?,
    isUploading: Boolean,
    showLocalBottomAction: Boolean,
    userScrollEnabled: Boolean,
    cameraActions: CameraActions,
    onNavigateToCameraPage: () -> Unit,
    onMoreClick: () -> Unit,
    onShowFriendSheet: () -> Unit = {},
    onRetryUpload: (String) -> Unit = {},
    onDeleteFailedPost: (String) -> Unit = {}
) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = userScrollEnabled,
        horizontalAlignment = Alignment.CenterHorizontally,
        beyondViewportPageCount = 1,
        key = { page ->
            when {
                page == CAMERA_PAGE_INDEX -> "camera"
                posts.isEmpty() -> "empty_media"
                else -> posts[page - 1].id
            }
        }) { page ->
        when (page) {
            CAMERA_PAGE_INDEX -> CameraPage(
                cameraState = cameraState,
                currentCaption = currentCaption,
                isUploading = isUploading,
                cameraActions = cameraActions
            )

            else -> {
                if (posts.isEmpty()) {
                    EmptyMediaPage(onAddFriendClick = onShowFriendSheet)
                } else {
                    val post = posts[page - 1]
                    MediaPage(
                        post = post,
                        uploadStatus = uploadStatuses[post.id],
                        showBottomAction = showLocalBottomAction,
                        showMoreButtonLoading = showMoreButtonLoading,
                        onGridClick = { /* TODO */ },
                        onCaptureClick = onNavigateToCameraPage,
                        onMoreClick = onMoreClick,
                        onRetryClick = { onRetryUpload(post.id) },
                        onDeleteClick = { onDeleteFailedPost(post.id) }
                    )
                }
            }
        }
    }
}