package com.thinh.snaplet.ui.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class OverlayViewModel @Inject constructor() : ViewModel() {

    private val _overlayState = MutableStateFlow<OverlayState>(OverlayState.None)
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()

    init {
        OverlayEventBus.events.onEach { event ->
            val current = _overlayState.value
            val currentIsBlocking = when (current) {
                is OverlayState.Visible.Modal -> current.isBlocking
                is OverlayState.Visible.BottomSheet -> current.isBlocking
                OverlayState.None -> false
            }

            when (event) {
                is OverlayEvent.ShowBottomSheet -> {
                    if (currentIsBlocking) return@onEach
                    _overlayState.update {
                        OverlayState.Visible.BottomSheet(
                            content = event.content,
                            onDismiss = event.onDismiss,
                            isBlocking = event.isBlocking,
                        )
                    }
                }

                is OverlayEvent.ShowModal -> {
                    if (currentIsBlocking) return@onEach
                    _overlayState.update {
                        OverlayState.Visible.Modal(
                            content = event.content,
                            onDismiss = event.onDismiss,
                            isBlocking = event.isBlocking,
                        )
                    }
                }

                is OverlayEvent.Dismiss -> {
                    if (currentIsBlocking && !event.force) return@onEach

                    (current as? OverlayState.Visible.Modal)?.onDismiss?.invoke()
                        ?: (current as? OverlayState.Visible.BottomSheet)?.onDismiss?.invoke()
                    _overlayState.value = OverlayState.None
                }
            }
        }.launchIn(viewModelScope)
    }

    fun dismiss() {
        OverlayEventBus.dismiss(force = false)
    }

    fun dismissForce() {
        OverlayEventBus.dismiss(force = true)
    }
}