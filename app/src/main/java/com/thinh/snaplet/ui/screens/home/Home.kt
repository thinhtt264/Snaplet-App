package com.thinh.snaplet.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.*
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.permission.Permission

private const val CAMERA_PAGE_INDEX = 0

@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PermissionHandler(
        permission = Permission.Camera,
        onPermissionResult = viewModel::onPermissionResult
    ) { requestPermission ->
        UiEventHandler(
            viewModel = viewModel,
            requestPermission = requestPermission
        )

        HomeContent(
            viewModel = viewModel,
            uiState = uiState
        )
    }
}

@Composable
private fun UiEventHandler(
    viewModel: HomeViewModel,
    requestPermission: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.onScreenInitialized()

        viewModel.uiEvent.collect { event ->
            handleUiEvent(event, requestPermission)
        }
    }
}

private fun handleUiEvent(
    event: HomeUiEvent,
    requestPermission: () -> Unit
) {
    when (event) {
        is HomeUiEvent.RequestPermission -> {
            Logger.d("🔐 Executing permission request from ViewModel")
            requestPermission()
        }

        is HomeUiEvent.ShowError -> {
            // TODO: Show error toast/snackbar
            Logger.e("⚠️ Error: ${event.message}")
        }

        is HomeUiEvent.ShowSuccess -> {
            // TODO: Show success toast/snackbar
            Logger.d("✅ Success: ${event.message}")
        }
    }
}

@Composable
private fun HomeContent(
    viewModel: HomeViewModel,
    uiState: HomeUiState
) {
    val context = LocalContext.current
    
    val pagerState = rememberPagerState(
        initialPage = CAMERA_PAGE_INDEX,
        pageCount = { 1 + uiState.mediaItems.size }
    )

    var shouldBindCamera by remember { mutableStateOf(true) }
    var captureSnapshotHandler by remember { mutableStateOf<(() -> Bitmap?)?>(null) }

    val cameraCallbacks = rememberCameraCallbacks(
        viewModel = viewModel,
        onSnapshotHandlerChanged = { handler ->
            captureSnapshotHandler = handler
        }
    )

    CameraBindingController(
        pagerState = pagerState,
        shouldBindCamera = shouldBindCamera,
        captureSnapshotHandler = captureSnapshotHandler,
        onShouldBindCameraChanged = { shouldBind -> shouldBindCamera = shouldBind },
        onSnapshotCaptured = viewModel::setPreviewSnapshot
    )

    MediaPager(
        pagerState = pagerState,
        uiState = uiState,
        context = context,
        viewModel = viewModel,
        shouldBindCamera = shouldBindCamera,
        cameraCallbacks = cameraCallbacks
    )
}

@Composable
private fun rememberCameraCallbacks(
    viewModel: HomeViewModel,
    onSnapshotHandlerChanged: ((() -> Bitmap?) -> Unit)
): CameraCallbacks {
    return remember {
        CameraCallbacks(
            onImageCaptureReady = { imageCapture ->
                viewModel.setImageCapture(imageCapture)
            },
            onSnapshotHandlerReady = { handler ->
                onSnapshotHandlerChanged(handler)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CameraBindingController(
    pagerState: PagerState,
    shouldBindCamera: Boolean,
    captureSnapshotHandler: (() -> Bitmap?)?,
    onShouldBindCameraChanged: (Boolean) -> Unit,
    onSnapshotCaptured: (Bitmap) -> Unit
) {
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        Logger.d("📄 Page: ${pagerState.currentPage}, Scrolling: ${pagerState.isScrollInProgress}")

        when {
            isOnCameraPage(pagerState) && !pagerState.isScrollInProgress -> {
                onShouldBindCameraChanged(true)
            }

            !isOnCameraPage(pagerState) && shouldBindCamera -> {
                captureAndSaveSnapshot(
                    captureSnapshotHandler = captureSnapshotHandler,
                    onSnapshotCaptured = onSnapshotCaptured
                )
                onShouldBindCameraChanged(false)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaPager(
    pagerState: PagerState,
    uiState: HomeUiState,
    context: Context,
    viewModel: HomeViewModel,
    shouldBindCamera: Boolean,
    cameraCallbacks: CameraCallbacks
) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        beyondViewportPageCount = 1,
        key = { page -> getPageKey(page, uiState) }
    ) { page ->
        when (page) {
            CAMERA_PAGE_INDEX -> {
                CameraPage(
                    cameraState = uiState.cameraState,
                    onImageCaptureReady = cameraCallbacks.onImageCaptureReady,
                    onSnapshotHandlerReady = cameraCallbacks.onSnapshotHandlerReady,
                    shouldBindCamera = shouldBindCamera,
                    onRequestPermission = viewModel::onScreenInitialized,
                    onCapturePhoto = { viewModel.onCapturePhoto(context) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                MediaPage(
                    page = page,
                    uiState = uiState
                )
            }
        }
    }
}

// Helper functions
@OptIn(ExperimentalFoundationApi::class)
private fun isOnCameraPage(pagerState: PagerState): Boolean {
    return pagerState.currentPage == CAMERA_PAGE_INDEX
}

private fun getPageKey(page: Int, uiState: HomeUiState): String {
    return if (page == CAMERA_PAGE_INDEX) {
        "camera_section"
    } else {
        uiState.mediaItems[page - 1].id
    }
}

private fun captureAndSaveSnapshot(
    captureSnapshotHandler: (() -> Bitmap?)?,
    onSnapshotCaptured: (Bitmap) -> Unit
) {
    captureSnapshotHandler?.invoke()?.let { bitmap ->
        onSnapshotCaptured(bitmap)
        Logger.d("✅ Snapshot saved to ViewModel")
    } ?: Logger.e("❌ Failed to capture snapshot")
}

// Data classes
private data class CameraCallbacks(
    val onImageCaptureReady: (ImageCapture) -> Unit,
    val onSnapshotHandlerReady: (() -> Bitmap?) -> Unit
)