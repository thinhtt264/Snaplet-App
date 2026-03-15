package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.R
import com.thinh.snaplet.data.model.Post
import com.thinh.snaplet.data.model.RelationshipStatus
import com.thinh.snaplet.data.model.RelationshipWithUser
import com.thinh.snaplet.data.model.media.ImageTransform
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.domain.feed.GetNewsfeedUseCase
import com.thinh.snaplet.domain.feed.ShouldTriggerLoadMoreUseCase
import com.thinh.snaplet.domain.media.DownloadPostImageUseCase
import com.thinh.snaplet.domain.media.ValidateCaptureReadinessUseCase
import com.thinh.snaplet.domain.model.CaptureReadiness
import com.thinh.snaplet.domain.model.PostAction
import com.thinh.snaplet.domain.model.UploadPostResult
import com.thinh.snaplet.domain.post.CreateTempPostUseCase
import com.thinh.snaplet.domain.post.DeletePostUseCase
import com.thinh.snaplet.domain.post.GetAvailablePostActionsUseCase
import com.thinh.snaplet.domain.post.UploadPostUseCase
import com.thinh.snaplet.domain.post.ValidateRetryUploadUseCase
import com.thinh.snaplet.domain.post.ValidateUploadPostUseCase
import com.thinh.snaplet.domain.user.AcceptFriendRequestUseCase
import com.thinh.snaplet.domain.user.GetDisplayableFriendsCountUseCase
import com.thinh.snaplet.domain.user.GetRelationshipActionUseCase
import com.thinh.snaplet.domain.user.GetRelationshipsByStatusesUseCase
import com.thinh.snaplet.domain.user.RemoveFriendUseCase
import com.thinh.snaplet.domain.user.RemoveRelationshipUseCase
import com.thinh.snaplet.domain.feed.ObserveNewPostEventUseCase
import com.thinh.snaplet.platform.permission.Permission
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.platform.permission.PermissionManager
import com.thinh.snaplet.platform.share.ShareApp
import com.thinh.snaplet.platform.share.ShareManager
import com.thinh.snaplet.ui.common.UiText
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.overlay.SheetOption
import com.thinh.snaplet.ui.theme.Error50
import com.thinh.snaplet.utils.FileUtils
import com.thinh.snaplet.utils.network.onFailure
import com.thinh.snaplet.utils.network.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
    private val getNewsfeedUseCase: GetNewsfeedUseCase,
    private val shouldTriggerLoadMoreUseCase: ShouldTriggerLoadMoreUseCase,
    private val validateCaptureReadinessUseCase: ValidateCaptureReadinessUseCase,
    private val createTempPostUseCase: CreateTempPostUseCase,
    private val validateUploadPostUseCase: ValidateUploadPostUseCase,
    private val uploadPostUseCase: UploadPostUseCase,
    private val validateRetryUploadUseCase: ValidateRetryUploadUseCase,
    private val getAvailablePostActionsUseCase: GetAvailablePostActionsUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val downloadPostImageUseCase: DownloadPostImageUseCase,
    private val getDisplayableFriendsCountUseCase: GetDisplayableFriendsCountUseCase,
    private val getRelationshipsByStatusesUseCase: GetRelationshipsByStatusesUseCase,
    private val getRelationshipActionUseCase: GetRelationshipActionUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val removeRelationshipUseCase: RemoveRelationshipUseCase,
    private val userRepository: UserRepository,
    private val shareManager: ShareManager,
    private val observeNewPostEvent: ObserveNewPostEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            cameraState = CameraState(
                hasCameraPermission = permissionManager.hasPermission(Permission.Camera)
            )
        )
    )

    val uiState: StateFlow<HomeUiState> = combine(
        _uiState,
        userRepository.observeMyUserProfile()
    ) { state, profile ->
        state.copy(profileAvatarUrl = profile?.avatarUrls?.forThumbnail().orEmpty())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _uiState.value
    )

    private val _imageCapture = mutableStateOf<ImageCapture?>(null)

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onPermissionRequestHandled() {
        _uiState.update { it.copy(pendingPermission = null) }
    }

    fun onScrollToFirstPostHandled() {
        _uiState.update { it.copy(shouldScrollToFirstPost = false) }
    }

    private var currentVisibleIndex: Int = -1
    private val currentPostVisible: Post?
        get() = _uiState.value.posts.getOrNull(currentVisibleIndex)

    /** Temp posts for retry only: lookup by id to get imagePath/transform/caption. Not in UI state. */
    private var tempPosts: List<Post> = emptyList()

    init {
        loadNewsfeed()
        loadFriendsCount()
        observeNewPostEvent()
            .onEach { event ->
                Logger.d("HomeViewModel: new_post received count=${event.count} seq=${event.seq}")
            }
            .launchIn(viewModelScope)
    }

    private fun updateFriendSheetState(transform: (FriendBottomSheetState) -> FriendBottomSheetState) {
        _uiState.update { it.copy(friendSheetState = transform(it.friendSheetState)) }
    }

    private fun loadFriendsCount() {
        viewModelScope.launch {
            getDisplayableFriendsCountUseCase().onSuccess { count ->
                updateFriendSheetState { it.copy(friendsCount = count) }
            }
        }
    }

    fun loadShareApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val apps = shareManager.getTopShareApps()
            updateFriendSheetState { it.copy(shareApps = apps) }
        }
    }

    fun loadMyFriendList() {
        viewModelScope.launch {
            updateFriendSheetState { it.copy(isLoadingFriendList = true) }
            getRelationshipsByStatusesUseCase(
                listOf(
                    RelationshipStatus.ACCEPTED, RelationshipStatus.PENDING
                )
            ).onSuccess { list ->
                val accepted = list.filter { it.status == RelationshipStatus.ACCEPTED }
                val pending = list.filter { it.status == RelationshipStatus.PENDING }
                val pendingWithActions = coroutineScope {
                    pending.map { item ->
                        async {
                            PendingListItemState(
                                item, getRelationshipActionUseCase(item.userId)
                            )
                        }
                    }.awaitAll()
                }
                updateFriendSheetState {
                    it.copy(
                        friendList = accepted,
                        pendingList = pendingWithActions,
                        isLoadingFriendList = false
                    )
                }
            }.onFailure {
                updateFriendSheetState { it.copy(isLoadingFriendList = false) }
            }
        }
    }

    fun acceptFriendRequest(pending: RelationshipWithUser) {
        viewModelScope.launch {
            acceptFriendRequestUseCase(pending.id).onSuccess {
                val acceptedRelationship = pending.copy(status = RelationshipStatus.ACCEPTED)
                updateFriendSheetState { state ->
                    state.copy(
                        pendingList = state.pendingList.filterNot { it.relationship.id == pending.id },
                        friendList = state.friendList + acceptedRelationship
                    )
                }
                loadFriendsCount()
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
            }
        }
    }

    fun requestRemoveFriend(friend: RelationshipWithUser) {
        OverlayEventBus.showConfirmDialog(
            title = UiText.StringResource(
                R.string.remove_friend_confirm_title,
                listOf(friend.displayName)
            ),
            message = UiText.StringResource(R.string.remove_friend_confirm_message),
            confirmText = UiText.StringResource(R.string.delete),
            cancelText = UiText.StringResource(R.string.cancel),
            confirmDestructive = true,
            onConfirm = { removeFriend(friend) },
        )
    }

    private fun removeFriend(friend: RelationshipWithUser) {
        viewModelScope.launch {
            val state = _uiState.value.friendSheetState
            if (friend.status == RelationshipStatus.PENDING) {
                removeRelationshipUseCase(friend.id).onSuccess {
                    updateFriendSheetState { s ->
                        s.copy(pendingList = s.pendingList.filterNot { it.relationship.id == friend.id })
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
                }
            } else {
                val currentCount = state.friendsCount
                removeFriendUseCase(friend.id, currentCount).onSuccess {
                    updateFriendSheetState { s ->
                        s.copy(friendList = s.friendList.filterNot { it.id == friend.id })
                    }
                    loadFriendsCount()
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
                }
            }
        }
    }

    fun shareToApp(app: ShareApp) {
        viewModelScope.launch {
            val userName =
                userRepository.getCurrentUserProfile()?.userName?.takeIf { it.isNotBlank() }
            val content = shareManager.buildInviteShareContent(userName)
            shareManager.shareToApp(app.packageName, content)
        }
    }

    fun shareOther() {
        viewModelScope.launch {
            val userName =
                userRepository.getCurrentUserProfile()?.userName?.takeIf { it.isNotBlank() }
            val content = shareManager.buildInviteShareContent(userName)
            shareManager.openSystemChooser(content)
        }
    }

    private fun loadNewsfeed(isLoadMore: Boolean = false) {
        val state = _uiState.value

        if (isLoadMore) {
            if (state.isLoadingMore || state.nextCursor == null) return
        } else if (state.isLoadingPosts) return

        viewModelScope.launch {
            _uiState.update {
                if (isLoadMore) it.copy(isLoadingMore = true)
                else it.copy(isLoadingPosts = true)
            }

            val cursor = if (isLoadMore) state.nextCursor else null

            getNewsfeedUseCase(cursor = cursor).fold(onSuccess = { feedData ->
                _uiState.update {
                    it.copy(
                        posts = if (isLoadMore) it.posts + feedData.data else feedData.data,
                        isLoadingPosts = false,
                        isLoadingMore = false,
                        nextCursor = feedData.pagination.nextCursor,
                        error = null
                    )
                }
            }, onFailure = { apiError ->
                _uiState.update {
                    it.copy(
                        isLoadingPosts = false, isLoadingMore = false, error = apiError.message
                    )
                }
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(apiError.message)) }
            })
        }
    }

    fun onItemVisible(currentIndex: Int) {
        currentVisibleIndex = currentIndex

        val state = _uiState.value
        val shouldLoad = shouldTriggerLoadMoreUseCase(
            currentIndex = currentIndex,
            totalItems = state.posts.size,
            canLoadMore = state.canLoadMore
        )
        if (shouldLoad) loadNewsfeed(isLoadMore = true)
    }

    fun onSwitchCamera() {
        updateCameraState { state ->
            val newLens = if (state.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
            state.copy(lensFacing = newLens, lastPreviewSnapshot = null)
        }
    }

    private fun updateCameraState(transform: (CameraState) -> CameraState) {
        _uiState.update { state ->
            state.copy(cameraState = transform(state.cameraState))
        }
    }

    fun updateCurrentCaption(caption: String) {
        _uiState.update { it.copy(currentCaption = caption) }
    }

    fun onRequestCameraPermission() {
        val hasPermission = permissionManager.hasPermission(Permission.Camera)
        updateCameraState { it.copy(hasCameraPermission = hasPermission) }

        if (!hasPermission) {
            _uiState.update { it.copy(pendingPermission = Permission.Camera) }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        updateCameraState { it.copy(hasCameraPermission = granted) }
        if (!granted) {
            _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Camera permission is required")) }
        }
    }

    fun setImageCapture(capture: ImageCapture) {
        _imageCapture.value = capture
        updateCameraState { it.copy(isCameraActive = true) }
    }

    fun setPreviewSnapshot(bitmap: Bitmap) {
        updateCameraState { it.copy(lastPreviewSnapshot = bitmap) }
    }

    fun onCameraPageVisible() {
        updateCameraState { it.copy(shouldBindCamera = true) }
    }

    fun onCameraPageHidden() {
        updateCameraState { it.copy(shouldBindCamera = false) }
    }

    fun onCapturePhoto(context: Context) {
        val cameraState = _uiState.value.cameraState
        when (validateCaptureReadinessUseCase(
            cameraState.hasCameraPermission, cameraState.isCameraActive
        )) {
            CaptureReadiness.NeedPermission -> _uiState.update { it.copy(pendingPermission = Permission.Camera) }
            CaptureReadiness.CameraNotReady -> _uiState.update {
                it.copy(snackbarMessage = UiText.DynamicString("Camera is not ready"))
            }

            CaptureReadiness.Ready -> takePhoto(context)
        }
    }

    private fun takePhoto(context: Context) {
        val capture = _imageCapture.value ?: run {
            _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Camera is not ready")) }
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        updateCameraState { it.copy(isCapturing = true) }

        val photoFile = File(
            context.cacheDir, SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        withContext(Dispatchers.Main.immediate) {
                            updateCameraState {
                                it.copy(
                                    isCapturing = false, capturedImagePath = photoFile.absolutePath
                                )
                            }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    updateCameraState { it.copy(isCapturing = false) }
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Failed to capture photo")) }
                }
            })
    }

    fun onCancelCapture() {
        val imagePath = _uiState.value.cameraState.capturedImagePath
        FileUtils.deleteFileFromPath(imagePath)
        updateCameraState { it.copy(capturedImagePath = null) }
        _uiState.update { it.copy(currentCaption = null) }
    }

    fun onUploadPost() {
        val state = _uiState.value
        viewModelScope.launch {
            val isUploading = state.uploadStatuses.values.any { it is UploadStatus.Uploading }
            when (val result = validateUploadPostUseCase(
                capturedImagePath = state.cameraState.capturedImagePath,
                caption = state.currentCaption,
                isUploading = isUploading
            )) {
                is ValidateUploadPostUseCase.ValidateUploadResult.Success -> {
                    val input = result.input
                    val tempPostId = "temp_${System.currentTimeMillis()}"

                    _uiState.update { s ->
                        s.copy(
                            uploadStatuses = s.uploadStatuses + (tempPostId to UploadStatus.Uploading),
                        )
                    }

                    val isFrontCamera =
                        state.cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT
                    val processedPath = withContext(Dispatchers.IO) {
                        FileUtils.flipAndCompressImage(
                            File(input.imagePath), flipHorizontal = isFrontCamera
                        ) ?: input.imagePath
                    }
                    val transform = ImageTransform(rotation = 0, scaleX = 1f, scaleY = 1f)
                    val tempPost = createTempPostUseCase(
                        id = tempPostId,
                        imagePath = processedPath,
                        userProfile = input.userProfile,
                        transform = transform,
                        caption = input.caption
                    )

                    tempPosts = tempPosts + tempPost
                    _uiState.update { s ->
                        s.copy(
                            posts = listOf(tempPost) + s.posts,
                            cameraState = s.cameraState.copy(capturedImagePath = null),
                            currentCaption = null
                        )
                    }

                    _uiState.update { it.copy(shouldScrollToFirstPost = true) }
                    runUploadAndUpdateStatus(tempPostId, processedPath, transform, input.caption)
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.AlreadyUploading -> { /* no-op, already uploading */
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.NoImage -> {
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("No image to upload")) }
                }

                is ValidateUploadPostUseCase.ValidateUploadResult.UserProfileNotFound -> {
                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("User profile not found")) }
                }
            }
        }
    }

    private fun runUploadAndUpdateStatus(
        tempPostId: String, imagePath: String, transform: ImageTransform, caption: String?
    ) {
        viewModelScope.launch {
            when (val result = uploadPostUseCase(imagePath, transform, caption)) {
                is UploadPostResult.Success -> {
                    val realPostId = result.post.id
                    tempPosts = tempPosts.filterNot { it.id == tempPostId }
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { if (it.id == tempPostId) it.copy(id = realPostId) else it },
                            uploadStatuses = state.uploadStatuses - tempPostId,
                            snackbarMessage = UiText.DynamicString("Post uploaded successfully")
                        )
                    }
                }

                is UploadPostResult.Failed -> {
                    setUploadStatus(tempPostId, UploadStatus.Failed(result.message))
//                    _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(result.message)) }
                }
            }
        }
    }

    fun onShowMoreOptions() {
        currentPostVisible?.let { onShowMoreOptions(it) }
    }

    fun onShowMoreOptions(post: Post) {
        val isUploading = _uiState.value.uploadStatuses[post.id] == UploadStatus.Uploading
        val actions = getAvailablePostActionsUseCase(post, isUploading)

        val options = actions.map { action ->
            when (action) {
                is PostAction.Share -> SheetOption(
                    id = "share",
                    label = UiText.StringResource(R.string.share),
                    onClick = { /* TODO: share */ })

                is PostAction.Download -> SheetOption(
                    id = "download",
                    label = UiText.StringResource(R.string.download),
                    onClick = { downloadPostImage(post) })

                is PostAction.Delete -> SheetOption(
                    id = "delete",
                    label = UiText.StringResource(R.string.delete),
                    color = Error50,
                    onClick = {
                        OverlayEventBus.showConfirmDialog(
                            title = UiText.StringResource(R.string.delete_photo_title),
                            message = UiText.StringResource(R.string.delete_photo_message),
                            confirmText = UiText.StringResource(R.string.delete),
                            onConfirm = { deletePost(post.id) },
                        )
                    })

                is PostAction.Report -> SheetOption(
                    id = "report",
                    label = UiText.StringResource(R.string.report),
                    color = Error50,
                    onClick = {})

                is PostAction.Cancel -> SheetOption(
                    id = "cancel",
                    label = UiText.StringResource(R.string.cancel),
                    onClick = { /* dismiss only */ })
            }
        }
        OverlayEventBus.showOptionsSheet(options = options)
    }

    fun retryUpload(tempPostId: String) {
        val tempPost = tempPosts.find { it.id == tempPostId }
        when (val result = validateRetryUploadUseCase(tempPost)) {
            is ValidateRetryUploadUseCase.ValidateRetryResult.Success -> {
                val input = result.input
                setUploadStatus(input.tempPostId, UploadStatus.Uploading)
                runUploadAndUpdateStatus(
                    input.tempPostId, input.imagePath, input.transform, input.caption
                )
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.PostNotFound -> {
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Cannot retry upload: Post data not found")) }
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.MediaNotFound -> {
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Cannot retry upload: Media not found")) }
            }

            is ValidateRetryUploadUseCase.ValidateRetryResult.ImagePathNotFound -> {
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString("Cannot retry upload: Image path not found")) }
            }
        }
    }

    private fun setUploadStatus(tempPostId: String, status: UploadStatus) {
        _uiState.update { state ->
            state.copy(uploadStatuses = state.uploadStatuses + (tempPostId to status))
        }
    }

    fun deleteFailedPost(tempPostId: String) {
        tempPosts = tempPosts.filterNot { it.id == tempPostId }
        removeTempPost(tempPostId)
    }

    private fun removeTempPost(tempPostId: String) {
        _uiState.update { state ->
            state.copy(
                posts = state.posts.filterNot { it.id == tempPostId },
                uploadStatuses = state.uploadStatuses - tempPostId
            )
        }
    }

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId).onSuccess {
                tempPosts = tempPosts.filterNot { it.id == postId }
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.filterNot { it.id == postId },
                        uploadStatuses = state.uploadStatuses - postId
                    )
                }
                _uiState.update { it.copy(snackbarMessage = UiText.StringResource(R.string.post_deleted)) }
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = UiText.DynamicString(error.message)) }
            }
        }
    }

    fun downloadPostImage(post: Post) {
        if (_uiState.value.isDownloading) return
        val media = post.media.firstOrNull() ?: run {
            _uiState.update { it.copy(snackbarMessage = UiText.StringResource(R.string.download_failed)) }
            return
        }
        val imageSource = media.images.original

        _uiState.update { it.copy(isDownloading = true) }
        viewModelScope.launch {
            downloadPostImageUseCase(imageSource).onSuccess {
                _uiState.update {
                    it.copy(
                        isDownloading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        snackbarMessage = UiText.DynamicString(e.message ?: "Download failed")
                    )
                }
            }
        }
    }

    fun downloadCaptureImage() {
        val state = _uiState.value
        val imageSource = state.cameraState.capturedImagePath
        if (state.isDownloading || imageSource == null) return

        _uiState.update { it.copy(isDownloading = true) }

        viewModelScope.launch {
            val isFrontCamera =
                state.cameraState.lensFacing == CameraSelector.LENS_FACING_FRONT

            val processedPath = withContext(Dispatchers.IO) {
                FileUtils.flipAndCompressImage(
                    File(imageSource), flipHorizontal = isFrontCamera
                ) ?: imageSource
            }

            downloadPostImageUseCase(processedPath).onSuccess {
                _uiState.update {
                    it.copy(
                        isDownloading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        snackbarMessage = UiText.DynamicString(e.message ?: "Download failed")
                    )
                }
            }
        }
    }
}