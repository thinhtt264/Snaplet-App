---
name: android-viewmodel
description: Best practices for implementing Android ViewModels following Google's strongly recommended architecture. StateFlow for UI state, process events in ViewModel (no event emission to UI), handling ApiResult from repository (fold vs onSuccess/onFailure).
---

# Android ViewModel & State Management

Follow [Android Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations) (strongly recommended).

## Instructions

Use `ViewModel` to hold state and business logic. It must outlive configuration changes.

### 1. UI State (StateFlow) — Single Source of Truth
*   **What**: Represents all UI state the screen needs (e.g., `Loading`, `Success(data)`, `Error`, snackbar message, navigation target).
*   **Type**: `StateFlow<UiState>`.
*   **Initialization**: Must have an initial value.
*   **Exposure**: Expose as read-only `StateFlow`. For streams from data layer, use `stateIn(scope, SharingStarted.WhileSubscribed(5000), initialValue)`.
    ```kotlin
    // From repository stream
    val uiState: StateFlow<NewsFeedUiState> = newsRepository
        .getNewsStream()
        .mapToUiState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NewsFeedUiState.Loading
        )

    // Or for mutable state
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    ```
*   **Updates**: Use `.update { oldState -> ... }` for thread safety.
*   **Single property**: Prefer one `uiState`; multiple unrelated properties are acceptable when justified.

### 2. Do NOT Send Events from ViewModel to UI (Strongly Recommended)
*   **Process events immediately in the ViewModel** and update state with the result.
*   **Avoid** `SharedFlow` / `Channel` for one-off events (toast, snackbar, navigation). Model them as state instead.
*   **Pattern**: Snackbar, toast, navigation target → include in `UiState`. UI observes state, reacts, then calls ViewModel to clear/consume.
    ```kotlin
    data class UiState(
        val items: List<Item>,
        val isLoading: Boolean = false,
        val snackbarMessage: String? = null  // UI shows, then calls onSnackbarDismissed
    )

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
    ```
*   **Reference**: [Handle ViewModel events](https://developer.android.com/topic/architecture/ui-layer/events#handle-viewmodel-events).

### 3. Lifecycle-Aware Collection in UI
*   **Compose**: Always use `collectAsStateWithLifecycle()` for `StateFlow`.
    ```kotlin
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ```
*   **Views (XML)**: Use `repeatOnLifecycle(Lifecycle.State.STARTED)` within a coroutine.

### 4. ViewModel Constraints (Strongly Recommended)
*   **Agnostic of Android lifecycle**: Do not hold `Activity`, `Fragment`, `Context`, or `Resources`. Use standard `ViewModel`, not `AndroidViewModel`.
*   **Screen-level only**: Use ViewModels for screen composables, Activities/Fragments, or navigation destinations—not in reusable UI components.
*   **Reusable UI**: Use plain state holder classes (not ViewModels) for complex reusable components; hoist state externally.

### 5. Scope
*   Use `viewModelScope` for all coroutines started by the ViewModel.
*   Delegate operations to UseCases or Repositories where appropriate.

### 6. Handling ApiResult / Result from repository

*   **fold**: use when you need to **use the returned data** — get one value (both success and failure return/transform to the same type).
*   **onSuccess / onFailure**: use to **transform or handle the result** per branch; when you don't need a single value from the result, use onSuccess/onFailure.

Example: need one value → `val x = result.fold(onSuccess = { it }, onFailure = { null })`. Only side effects → `result.onSuccess { ... }.onFailure { ... }`.

*   **Use `fold`** when you need to **produce a single value** (e.g. assign to `_uiState.value`) and **both** success and failure branches **return that same type** (e.g. `UiState.Success(data)` vs `UiState.Error(message)`).
*   **Use `onSuccess` / `onFailure`** when you only need **side effects** (update state per branch, set snackbar in state, etc.) and do **not** need one derived value.

### 7. Real-time `Flow` events from repository (unread/new messages)
*   When a repository exposes a long-lived `Flow` of updates (e.g. realtime socket/websocket events), process them inside the ViewModel and update `uiState` accordingly.
*   Prefer structured handling:
    *   Use `onEach { ... }.launchIn(viewModelScope)` (or a derived `stateIn` when the UI state is directly representable as a stream).
    *   If events include ordering metadata (e.g. `seq`), gate/ignore out-of-order or duplicate events in the ViewModel.
*   If an event triggers an in-flight refresh/fetch:
    *   Cancel the previous job before starting a new one to avoid races (e.g. `newerJob?.cancel()` then `newerJob = viewModelScope.launch { ... }`).
    *   Keep the fetch/orchestration in use cases; the ViewModel should mainly coordinate state transitions and job lifecycles.
