package com.thinh.snaplet.platform.app

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level visibility (MainActivity onStart/onStop).
 * Emits [enteredForeground] only on background → foreground transitions.
 */
@Singleton
class AppVisibilityTracker @Inject constructor() {

    private val _isForegrounded = MutableStateFlow(false)
    val isForegrounded: StateFlow<Boolean> = _isForegrounded.asStateFlow()

    private val _enteredForeground = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val enteredForeground: SharedFlow<Unit> = _enteredForeground.asSharedFlow()

    fun setAppVisible(isVisible: Boolean) {
        val wasVisible = _isForegrounded.value
        _isForegrounded.value = isVisible
        if (isVisible && !wasVisible) {
            _enteredForeground.tryEmit(Unit)
        }
    }
}
