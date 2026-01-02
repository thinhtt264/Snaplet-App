package com.thinh.snaplet.navigation

sealed class NavScreen(val route: String) {
    data object AuthGraph : NavScreen("auth_graph")
    data object HomeGraph : NavScreen("home_graph")

    data object Onboarding : NavScreen("onboarding")
    data object Home : NavScreen("home")
    data object Login : NavScreen("login")
}