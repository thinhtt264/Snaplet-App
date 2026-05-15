package com.thinh.snaplet.domain.chat

import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlinePresenceController @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    private val _onlineUserIds = MutableStateFlow<Set<String>>(emptySet())
    val onlineUserIds: StateFlow<Set<String>> = _onlineUserIds.asStateFlow()

    private var realtimeJob: Job? = null

    fun refresh(scope: CoroutineScope) {
        scope.launch {
            when (val result = chatRepository.fetchOnlineFriends()) {
                is ApiResult.Success -> _onlineUserIds.update { result.data.toSet() }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun startRealtimeUpdates(scope: CoroutineScope) {
        if (realtimeJob?.isActive == true) return
        realtimeJob = scope.launch {
            launch {
                chatRepository.observePartnerOnline().collect { payload ->
                    _onlineUserIds.update { it + payload.userId }
                }
            }
            launch {
                chatRepository.observePartnerOffline().collect { payload ->
                    _onlineUserIds.update { it - payload.userId }
                }
            }
        }
    }

    fun clear() {
        _onlineUserIds.value = emptySet()
    }
}
