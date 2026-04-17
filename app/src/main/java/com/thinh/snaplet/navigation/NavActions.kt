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

    fun navigateToConversationList() {
        nav.navigate(ConversationList)
    }

    fun navigateToChatConversation(
        conversationId: String,
        partnerName: String,
        partnerAvatarUrl: String?,
    ) {
        nav.navigate(
            ChatConversation(
                conversationId = conversationId,
                partnerName = partnerName,
                partnerAvatarUrl = partnerAvatarUrl,
            )
        )
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

    fun navigateToRegister(
        firstName: String? = null,
        lastName: String? = null,
        isFromGoogleLogin: Boolean = false
    ) {
        nav.navigate(Register(firstName = firstName, lastName = lastName, isFromGoogleLogin)) {
            popUpTo<Login> { inclusive = true }
        }
    }

    fun navigateToLogin() {
        nav.navigate(Login) {
            popUpTo<Register> { inclusive = true }
        }
    }

    fun navigateToOnboarding() {
        nav.navigate(Onboarding) {
            popUpTo<AuthGraph> { inclusive = false }
            launchSingleTop = true
        }
    }
}
