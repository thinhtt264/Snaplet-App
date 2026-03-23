package com.thinh.snaplet.ui.overlay

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.thinh.snaplet.ui.overlay.bottom_sheet.GlobalBottomSheet
import com.thinh.snaplet.ui.overlay.modal.GlobalModal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayHost() {
    val overlayViewModel: OverlayViewModel = hiltViewModel()
    val overlayState by overlayViewModel.overlayState.collectAsState()

    when (val current = overlayState) {
        is OverlayState.None -> Unit
        is OverlayState.Visible.Modal -> GlobalModal(
            content = current.content,
            onDismiss = if (current.isBlocking) overlayViewModel::dismissForce else overlayViewModel::dismiss
        )

        is OverlayState.Visible.BottomSheet ->
            GlobalBottomSheet(
                content = current.content,
                onDismiss = if (current.isBlocking) overlayViewModel::dismissForce else overlayViewModel::dismiss,
            )
    }
}