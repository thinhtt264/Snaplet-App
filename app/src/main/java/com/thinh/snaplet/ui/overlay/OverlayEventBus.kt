package com.thinh.snaplet.ui.overlay

import com.thinh.snaplet.ui.common.UiText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object OverlayEventBus {

    private val _events = MutableSharedFlow<OverlayEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    internal val events: Flow<OverlayEvent> = _events.asSharedFlow()

    fun showOptionsSheet(
        options: List<SheetOption>,
        title: UiText? = null,
        onDismiss: (() -> Unit)? = null,
    ) {
        _events.tryEmit(
            OverlayEvent.ShowBottomSheet(
                content = BottomSheetContent.Options(title = title, options = options),
                onDismiss = onDismiss,
            )
        )
    }

    fun showConfirmSheet(
        content: BottomSheetContent.Confirm,
        onDismiss: (() -> Unit)? = null,
    ) {
        _events.tryEmit(
            OverlayEvent.ShowBottomSheet(
                content = content,
                onDismiss = onDismiss,
            )
        )
    }

    fun showConfirmDialog(
        title: UiText,
        message: UiText,
        confirmText: UiText,
        cancelText: UiText? = null,
        onConfirm: () -> Unit,
        onDismiss: (() -> Unit)? = null,
    ) {
        showModal(
            content = ModalContent.ConfirmDialog(
                title = title,
                message = message,
                confirmText = confirmText,
                cancelText = cancelText,
                onConfirm = onConfirm
            ),
            onDismiss = onDismiss,
        )
    }

    fun showModal(
        content: ModalContent,
        onDismiss: (() -> Unit)? = null,
    ) {
        _events.tryEmit(OverlayEvent.ShowModal(content = content, onDismiss = onDismiss))
    }

    fun dismiss() {
        _events.tryEmit(OverlayEvent.Dismiss)
    }
}