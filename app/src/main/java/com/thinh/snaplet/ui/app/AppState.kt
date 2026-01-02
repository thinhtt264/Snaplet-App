sealed interface AppState {
    object Loading : AppState
    object Unauthenticated : AppState
    object Authenticated : AppState
    object ForceUpdate : AppState
}
