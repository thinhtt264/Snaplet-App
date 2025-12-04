package com.thinh.snaplet.navigation

sealed class NavScreen(val route: String) {
    data object Onboarding : NavScreen("onboarding")

    data object Home : NavScreen("home/{userName}") {
        fun createRoute(userName: String): String {
            return "home/$userName"
        }
    }
}

