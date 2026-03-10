package com.thinh.snaplet.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.rememberNavController
import com.chuckerteam.chucker.api.Chucker
import com.thinh.snaplet.BuildConfig
import com.thinh.snaplet.navigation.AuthGraph
import com.thinh.snaplet.navigation.HomeGraph
import com.thinh.snaplet.navigation.NavGraph
import com.thinh.snaplet.ui.app.AppUiEvent
import com.thinh.snaplet.ui.app.AppViewModel
import com.thinh.snaplet.ui.overlay.OverlayHost
import com.thinh.snaplet.ui.theme.SnapletTheme
import pressScaleClickable

@Composable
fun MainScreen(
    appViewModel: AppViewModel
) {
    SnapletTheme {
        val appUiState by appViewModel.uiState.collectAsState()

        val navController = rememberNavController()

        LaunchedEffect(Unit) {
            appViewModel.uiEvent.collect { event ->
                when (event) {
                    is AppUiEvent.NavigateToAuthGraph -> {
                        navController.navigate(AuthGraph) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    is AppUiEvent.NavigateToHomeGraph -> {
                        navController.navigate(HomeGraph) {
                            popUpTo<AuthGraph> { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
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

                if (BuildConfig.IS_DEVELOPMENT) {
                    val context = LocalContext.current
                    Box(
                        modifier = Modifier
                            .zIndex(1000f)
                            .align(Alignment.TopCenter)
                            .offset(y = 28.dp)
                            .size(28.dp)
                            .pressScaleClickable {
                                try {
                                    val chuckerIntent = Chucker.getLaunchIntent(context)
                                    context.startActivity(chuckerIntent)
                                } catch (_: Exception) {
                                    // Handle error silently
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Open Chucker",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}