package com.thinh.snaplet.navigation

import androidx.navigation.NavController

class NavigationManager(private val navController: NavController) {

    /**
     * Navigate to Home screen
     * @param userName Optional user name to pass to Home screen
     * @param clearBackStack Clear all back stack if true
     */
    fun navigateToHome(userName: String? = null, clearBackStack: Boolean = true) {
        val route = if (userName != null) {
            NavScreen.Home.createRoute(userName)
        } else {
            NavScreen.Home.route
        }
        
        if (clearBackStack) {
            navController.navigate(route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(route)
        }
    }

    /**
     * Navigate to Onboarding screen
     * @param clearBackStack Clear all back stack if true
     */
    fun navigateToOnboarding(clearBackStack: Boolean = true) {
        if (clearBackStack) {
            navController.navigate(NavScreen.Onboarding.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(NavScreen.Onboarding.route)
        }
    }

    /**
     * Navigate back to previous screen
     * @return true if navigation was successful, false if back stack is empty
     */
    fun navigateBack(): Boolean {
        return navController.popBackStack()
    }

    /**
     * Get current route
     * @return Current destination route or null
     */
    fun getCurrentRoute(): String? {
        return navController.currentBackStackEntry?.destination?.route
    }
}

