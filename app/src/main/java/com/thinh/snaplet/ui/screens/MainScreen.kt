package com.thinh.snaplet.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.thinh.snaplet.navigation.AuthGraph
import com.thinh.snaplet.navigation.HomeGraph
import com.thinh.snaplet.navigation.NavGraph
import com.thinh.snaplet.navigation.SpotlightPost
import com.thinh.snaplet.ui.app.AppUiEvent
import com.thinh.snaplet.ui.app.AppViewModel
import com.thinh.snaplet.ui.overlay.OverlayHost
import com.thinh.snaplet.ui.theme.SnapletTheme
import com.thinh.snaplet.utils.CrashlyticsLogger

@Composable
fun MainScreen(
    appViewModel: AppViewModel
) {
    SnapletTheme {
        val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()

        val navController = rememberNavController()

        LaunchedEffect(Unit) {
            appViewModel.uiEvent.collect { event ->
                when (event) {
                    is AppUiEvent.NavigateToAuthGraph -> {
                        navController.navigate(AuthGraph) {
                            popUpTo(navController.graph.id) {
                                inclusive = true
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }

                    is AppUiEvent.NavigateToHomeGraph -> {
                        navController.navigate(HomeGraph) {
                            popUpTo<AuthGraph> { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    is AppUiEvent.NavigateToSpotlightPost -> {
                        navController.navigate(SpotlightPost(postId = event.postId))
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            navController.currentBackStackEntryFlow.collect { entry ->
                val route = entry.destination.route ?: return@collect
                CrashlyticsLogger.screen(route)
            }
        }

        OverlayHost()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                appUiState.startDestination?.let {
                    NavGraph(
                        startDestination = it,
                        navController = navController,
                        modifier = Modifier
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}