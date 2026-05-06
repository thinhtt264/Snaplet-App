package com.thinh.snaplet.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.thinh.snaplet.ui.screens.chat.ChatScreen
import com.thinh.snaplet.ui.screens.conversation_list.ConversationListScreen
import com.thinh.snaplet.ui.screens.home.Home
import com.thinh.snaplet.ui.screens.image_crop.ImageCrop
import com.thinh.snaplet.ui.screens.login.Login
import com.thinh.snaplet.ui.screens.my_profile.MyProfile
import com.thinh.snaplet.ui.screens.onboarding.Onboarding
import com.thinh.snaplet.ui.screens.register.Register
import com.thinh.snaplet.ui.screens.spotlight_post.SpotlightPostScreen
import com.thinh.snaplet.navigation.ImageCrop as ImageCropRoute

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Any = AuthGraph,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = NavTransitions.App.enter,
        exitTransition = NavTransitions.App.exit,
        popEnterTransition = { EnterTransition.None },
        popExitTransition = NavTransitions.Default.popExit
    ) {
        authGraph(navController = navController)
        homeGraph(navController = navController)
    }
}

fun NavGraphBuilder.homeGraph(navController: NavHostController) {
    val actions = NavActions(navController)
    navigation<HomeGraph>(startDestination = Home) {
        composable<Home> {
            Home(
                onProfileClick = actions::navigateToMyProfile,
                onChatClick = actions::navigateToConversationList,
            )
        }
        composable<MyProfile>(
            enterTransition = NavTransitions.MyProfile.enter,
            popExitTransition = NavTransitions.MyProfile.popExit
        ) {
            MyProfile(
                onBackClick = actions::popBackStack,
                onNavigateToImageCrop = actions::navigateToImageCrop,
            )
        }
        composable<ImageCropRoute> {
            ImageCrop(onCropDone = { croppedUri ->
                actions.sendResultToPreviousScreen(
                    NavResultKeys.CroppedUri, croppedUri.toString()
                )
                actions.popBackStack()
            }, onBack = actions::popBackStack)
        }
        composable<SpotlightPost>(
            enterTransition = NavTransitions.Default.enter,
            popExitTransition = NavTransitions.Default.popExit,
        ) {
            SpotlightPostScreen(
                onNavigateBack = actions::popBackStack,
                onNavigateHome = actions::navigateToHome,
            )
        }
        composable<ConversationList>(
            exitTransition = { ExitTransition.None },
            popExitTransition = NavTransitions.Default.popExit,
        ) {
            ConversationListScreen(
                onNavigateBack = actions::popBackStack,
                onConversationClick = { conversation ->
                    actions.navigateToChatConversation(
                        ChatConversation(
                            conversationId = conversation.id,
                            partnerName = conversation.participantName,
                            partnerAvatarUrl = conversation.participantAvatarUrl,
                            partnerLastReadAtMs = conversation.partnerLastSeenAt,
                            myLastReadAtMs = conversation.myLastSeenAt,
                        )
                    )
                },
                onNavigateToNewChat = actions::navigateToChatConversation,
                onAddFriendClick = {
                    actions.sendResultToPreviousScreen(NavResultKeys.OpenFriendSheet, true)
                    actions.popBackStack()
                },
            )
        }
        composable<ChatConversation>(
            enterTransition = NavTransitions.Default.enter,
            popExitTransition = NavTransitions.Default.popExit,
        ) {
            ChatScreen(onNavigateBack = actions::popBackStack)
        }
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    val actions = NavActions(navController)
    navigation<AuthGraph>(startDestination = Onboarding) {
        composable<Onboarding> {
            Onboarding(
                onNavigateToLogin = actions::navigateToLoginReplacingOnboarding,
                onNavigateToRegister = actions::navigateToRegister
            )
        }
        composable<Login> {
            Login(
                onNavigateToOnboarding = actions::navigateToOnboarding,
                onRegisterClick = actions::navigateToRegister
            )
        }
        composable<Register> {
            Register(
                onLoginClick = actions::navigateToLogin,
                onNavigateToOnboarding = actions::navigateToOnboarding
            )
        }
    }
}
