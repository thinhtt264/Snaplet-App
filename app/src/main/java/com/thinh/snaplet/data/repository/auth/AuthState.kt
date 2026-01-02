sealed interface AuthState {
    object Authenticated : AuthState
    object Unauthenticated : AuthState
}