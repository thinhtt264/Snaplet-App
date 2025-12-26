package com.thinh.snaplet.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.thinh.snaplet.navigation.NavGraph
import com.thinh.snaplet.ui.screens.friend_request.FriendRequestOverlayScreen
import com.thinh.snaplet.ui.theme.SnapletTheme


@Composable
fun MainScreen() {
    SnapletTheme {
        val navController = rememberNavController()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                NavGraph(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding)
                )
                
                // Friend request overlay (shown on top when deeplink triggered)
                FriendRequestOverlayScreen()
            }
        }
    }
}