package com.thinh.snaplet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.thinh.snaplet.ui.screens.home.Home
import com.thinh.snaplet.ui.screens.onboarding.Onboarding

/**
 * NavGraph - Central navigation configuration Setup các routes và
 * destinations cho toàn bộ app
 */
@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: String = NavScreen.Onboarding.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavScreen.Onboarding.route) {
            Onboarding(
                onNavigateToHome = { userName ->
                    val route = NavScreen.Home.createRoute(userName)
                    navController.navigate(route) {
                        popUpTo(NavScreen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavScreen.Home.route,
        ) { Home() }
    }
}

