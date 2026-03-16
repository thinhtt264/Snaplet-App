package com.thinh.snaplet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.thinh.snaplet.platform.deeplink.DeepLinkManager
import com.thinh.snaplet.ui.app.AppViewModel
import com.thinh.snaplet.ui.screens.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var deepLinkManager: DeepLinkManager

    override fun onStart() {
        super.onStart()
        appViewModel.onAppVisibilityChanged(isVisible = true)
    }

    override fun onStop() {
        appViewModel.onAppVisibilityChanged(isVisible = false)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { appViewModel.uiState.value.isLoading }
        }

        super.onCreate(savedInstanceState)

        lifecycleScope.launch { deepLinkManager.handleDeepLink(intent) }

        enableEdgeToEdge()
        setContent { MainScreen(appViewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lifecycleScope.launch { deepLinkManager.handleDeepLink(intent) }
    }
}