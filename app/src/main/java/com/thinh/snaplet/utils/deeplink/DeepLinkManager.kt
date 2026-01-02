package com.thinh.snaplet.utils.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkManager @Inject constructor() {
    
    private val _events = MutableSharedFlow<DeepLinkEvent>(
        replay = 1,              // Replay last event to new subscribers (fix cold start)
        extraBufferCapacity = 1  // Buffer 1 additional event if processing is slow
    )

    val events: SharedFlow<DeepLinkEvent> = _events.asSharedFlow()

    suspend fun emitEvent(event: DeepLinkEvent) {
        _events.emit(event)
    }
}

