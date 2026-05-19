package com.thinh.snaplet.utils

import com.thinh.snaplet.platform.app.AppVisibilityTracker
import com.thinh.snaplet.platform.network.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared stale-refresh policy for remote data:
 * - App background → foreground: refresh only if data is older than [RefreshSlot.staleTimeMs].
 * - Network reconnect: refresh immediately (same as chat).
 */
@Singleton
class RemoteStaleRefreshCoordinator @Inject constructor(
    private val appVisibilityTracker: AppVisibilityTracker,
    private val connectivityObserver: ConnectivityObserver,
) {

    data class RefreshSlot(
        val id: String,
        val staleTimeMs: Long,
        val isLoading: () -> Boolean = { false },
        val canRefresh: () -> Boolean = { true },
        val onRefresh: () -> Unit,
    )

    private val lastSuccessAtMs = mutableMapOf<String, Long>()

    fun markSuccess(id: String) {
        lastSuccessAtMs[id] = System.currentTimeMillis()
    }

    fun observe(scope: CoroutineScope, slots: List<RefreshSlot>) {
        scope.launch {
            appVisibilityTracker.enteredForeground.collect {
                slots.forEach { tryForegroundRefresh(it) }
            }
        }
        scope.launch {
            connectivityObserver.isInternetAvailable
                .filter { it }
                .drop(1)
                .collect {
                    slots.forEach { tryNetworkRefresh(it) }
                }
        }
    }

    private fun tryForegroundRefresh(slot: RefreshSlot) {
        if (!slot.canRefresh() || slot.isLoading()) return
        val lastAt = lastSuccessAtMs[slot.id] ?: return
        if (System.currentTimeMillis() - lastAt < slot.staleTimeMs) return
        slot.onRefresh()
    }

    private fun tryNetworkRefresh(slot: RefreshSlot) {
        if (!slot.canRefresh() || slot.isLoading()) return
        slot.onRefresh()
    }
}
