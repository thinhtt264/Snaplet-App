package com.thinh.snaplet.ui.screens.home

import CameraPage
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.ui.components.PermissionHandler
import com.thinh.snaplet.ui.screens.home.components.MediaItemPage
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.permission.Permission

@Composable
fun Home(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    PermissionHandler(
        permission = Permission.Camera, onPermissionResult = { granted ->
            viewModel.onPermissionResult(granted)
        }) { requestPermission ->
        LaunchedEffect(Unit) {
            viewModel.onScreenInitialized()

            viewModel.uiEvent.collect { event ->
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
        }

        HomeContent(viewModel, uiState)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeContent(
    viewModel: HomeViewModel, uiState: HomeUiState
) {
    val onImageCaptureReady = remember {
        { imageCapture: ImageCapture ->
            viewModel.setImageCapture(imageCapture)
            Logger.d("ImageCapture ready and set to ViewModel")
        }
    }

    val totalPages = 1 + uiState.mediaItems.size

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { totalPages }
    )

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        key = { page ->
            if (page == 0) "camera_section" else uiState.mediaItems[page - 1].id
        }
    ) { page ->
        when (page) {
            0 -> {
                CameraPage(
                    viewModel = viewModel,
                    uiState = uiState,
                    onImageCaptureReady = onImageCaptureReady,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                val mediaIndex = page - 1
                if (mediaIndex < uiState.mediaItems.size) {
                    MediaItemPage(
                        media = uiState.mediaItems[mediaIndex],
                        onMediaClick = { media ->
                            Logger.d("📷 Clicked media: ${media.id}")
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Optional: Add page indicator or current page info
    LaunchedEffect(pagerState.currentPage) {
        Logger.d("📄 Current page: ${pagerState.currentPage}/${totalPages - 1}")
    }
}

