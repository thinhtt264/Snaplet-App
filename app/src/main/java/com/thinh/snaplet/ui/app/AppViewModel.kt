package com.thinh.snaplet.ui.app

import AuthState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.data.repository.device.DeviceRepository
import com.thinh.snaplet.navigation.AuthGraph
import com.thinh.snaplet.navigation.HomeGraph
import com.thinh.snaplet.platform.deeplink.DeepLinkEvent
import com.thinh.snaplet.platform.deeplink.DeepLinkManager
import com.thinh.snaplet.platform.socket.SocketManager
import com.thinh.snaplet.platform.widget.WidgetUpdateManager
import com.thinh.snaplet.ui.overlay.ModalContent
import com.thinh.snaplet.ui.overlay.OverlayEventBus
import com.thinh.snaplet.ui.screens.friend_request.FriendRequestUiState
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
    private val deepLinkManager: DeepLinkManager,
    private val userRepository: UserRepository,
    private val socketManager: SocketManager,
    private val widgetUpdateManager: WidgetUpdateManager,
) : ViewModel() {

    private val _uiState: MutableStateFlow<AppUiState> = MutableStateFlow(AppUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AppUiEvent>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.SUSPEND
    )
    val uiEvent = _uiEvent.asSharedFlow()

    private var isInitialized = false
    private val isAuthenticated = MutableStateFlow(false)
    private val isForegrounded = MutableStateFlow(false)

    /** Pending friend request deeplink (userName) to show after login in this session. */
    private var pendingFriendRequestUserName: String? = null

    init {
        initializeApp()
        observeAuthState()
        observeSocketSync()
        observerIsAuthenticated()
    }

    /**
     * Called by MainActivity when app visibility changes (onStart/onStop).
     * We always update foreground state; actual connect/disconnect is handled
     * reactively by [observeSocketSync] based on auth + foreground.
     */
    fun onAppVisibilityChanged(isVisible: Boolean) {
        isForegrounded.value = isVisible
    }

    private fun observerIsAuthenticated() {
        isAuthenticated.onEach {
            if (!isInitialized) return@onEach
            widgetUpdateManager.scheduleImmediateUpdate()
        }.launchIn(viewModelScope)
    }

    private fun observeSocketSync() {
        combine(isAuthenticated, isForegrounded) { authenticated, foregrounded ->
            authenticated && foregrounded
        }.onEach { shouldConnect ->
            if (shouldConnect) {
                viewModelScope.launch {
                    socketManager.connect()
                }
            } else {
                socketManager.disconnect()
            }
        }.launchIn(viewModelScope)
    }

    private fun initializeApp() {
        viewModelScope.launch {
            try {
                deviceRepository.getOrCreateFingerprint()

                val authenticated = authRepository.isAuthenticated()

                isAuthenticated.value = authenticated

                _uiState.update {
                    it.copy(
                        startDestination = if (authenticated) HomeGraph else AuthGraph
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(startDestination = AuthGraph) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                isInitialized = true
                observeDeepLinkEvents()
            }
        }
    }

    private fun observeAuthState() {
        authRepository.authState.onEach { authState ->
            if (!isInitialized) return@onEach
            when (authState) {
                is AuthState.Authenticated -> {
                    isAuthenticated.value = true
                    _uiEvent.emit(AppUiEvent.NavigateToHomeGraph)
                    pendingFriendRequestUserName?.let { userName ->
                        pendingFriendRequestUserName = null
                        handleFriendRequestDeepLink(userName)
                    }
                }

                is AuthState.Unauthenticated -> {
                    isAuthenticated.value = false
                    _uiEvent.emit(AppUiEvent.NavigateToAuthGraph)
                }
            }
        }.catch { e ->
            if (isInitialized) {
                isAuthenticated.value = false
                _uiEvent.emit(AppUiEvent.NavigateToAuthGraph)
            }
        }.launchIn(viewModelScope)
    }

    private fun observeDeepLinkEvents() {
        viewModelScope.launch {
            deepLinkManager.events.collect { event ->
                if (event is DeepLinkEvent.FriendRequest) {
                    handleFriendRequestDeepLink(event.userName)
                }
            }
        }
    }

    private suspend fun handleFriendRequestDeepLink(userName: String) {
        if (!authRepository.isAuthenticated()) {
            pendingFriendRequestUserName = userName
            return
        }
        val profileResult = userRepository.getUserProfile(userName)
        profileResult.fold(onSuccess = { userProfile ->
            val state = FriendRequestUiState(userProfile = userProfile)
            OverlayEventBus.showModal(ModalContent.FriendRequest(state = state))
        }, onFailure = { error ->
            Logger.e("❌ Failed to load user profile: ${error.message}")
        })
    }
}