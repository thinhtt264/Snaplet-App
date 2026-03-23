package com.thinh.snaplet.ui.overlay.modal

import androidx.compose.runtime.Composable
import com.thinh.snaplet.ui.overlay.ModalContent

@Composable
internal fun GlobalModal(
    content: ModalContent,
    onDismiss: () -> Unit,
) {
    when (content) {
        is ModalContent.ConfirmDialog -> ConfirmDialog(
            content = content,
            onDismiss = onDismiss
        )

        is ModalContent.ForceLogoutDialog -> ForceLogoutModal(
            content = content,
            onDismiss = onDismiss
        )

        is ModalContent.FriendRequest -> FriendRequestModal(
            content = content,
            onDismiss = onDismiss
        )
    }
}