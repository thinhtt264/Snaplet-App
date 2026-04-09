package com.thinh.snaplet.navigation

import android.net.Uri
import androidx.navigation.NavHostController

class NavActions(
    private val nav: NavHostController
) {
    fun <T : Any> sendResultToPreviousScreen(key: NavResultKey<T>, value: T) {
        nav.previousBackStackEntry?.savedStateHandle?.set(key.key, value)
    }

    fun navigateToMyProfile() {
        nav.navigate(MyProfile)
    }

    fun navigateToHome() {
        nav.navigate(Home) {
            popUpTo<HomeGraph> { inclusive = false }
            launchSingleTop = true
        }
    }

    fun navigateToImageCrop(uri: Uri) {
        nav.navigate(ImageCrop(sourceUri = uri.toString()))
    }

    fun popBackStack(): Boolean = nav.popBackStack()

    fun navigateToLoginReplacingOnboarding() {
        nav.navigate(Login) {
            popUpTo<Onboarding> { inclusive = true }
        }
    }

    fun navigateToRegisterReplacingOnboarding() {
        nav.navigate(Register) {
            popUpTo<Onboarding> { inclusive = true }
        }
    }

    fun navigateToRegister() {
        nav.navigate(Register)
    }

    fun navigateToLogin() {
        nav.navigate(Login)
    }
}
