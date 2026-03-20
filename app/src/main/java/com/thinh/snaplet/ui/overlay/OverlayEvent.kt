package com.thinh.snaplet.ui.overlay

sealed interface OverlayEvent {
    data class ShowBottomSheet(
        val content: BottomSheetContent,
        val onDismiss: (() -> Unit)? = null,
        val isBlocking: Boolean = false,
    ) : OverlayEvent

    data class ShowModal(
        val content: ModalContent,
        val onDismiss: (() -> Unit)? = null,
        val isBlocking: Boolean = false,
    ) : OverlayEvent

    data class Dismiss(
        val force: Boolean = false,
    ) : OverlayEvent
}
