package com.thinh.snaplet

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.thinh.snaplet.ui.screens.MainScreen
import com.thinh.snaplet.ui.screens.splash.SplashViewModel
import com.thinh.snaplet.utils.Logger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { splashViewModel.isLoading.value }
        }

        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        enableEdgeToEdge()
        setContent { MainScreen() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Logger.d("🔗 onNewIntent called")

        setIntent(intent)

        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.data

            if (data != null) {
                val userName = data.getQueryParameter("userName")

                data.queryParameterNames.forEach { paramName ->
                    val paramValue = data.getQueryParameter(paramName)
                    Logger.d("📝 Param: $paramName = $paramValue")
                }
            }
        }
    }
}