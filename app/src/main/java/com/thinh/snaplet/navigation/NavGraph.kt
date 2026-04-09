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
import com.thinh.snaplet.ui.screens.home.Home
import com.thinh.snaplet.ui.screens.image_crop.ImageCrop
import com.thinh.snaplet.ui.screens.login.Login
import com.thinh.snaplet.ui.screens.my_profile.MyProfile
import com.thinh.snaplet.ui.screens.onboarding.Onboarding
import com.thinh.snaplet.ui.screens.spotlight_post.SpotlightPostScreen
import com.thinh.snaplet.ui.screens.register.Register
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
    navigation<HomeGraph>(
        startDestination = Home,
//        enterTransition = NavTransitions.HomeGraph.enter,
//        popExitTransition = NavTransitions.HomeGraph.exit
    ) {
        composable<Home> {
            Home(onProfileClick = actions::navigateToMyProfile)
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
                    NavResultKeys.CroppedUri,
                    croppedUri.toString()
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
    }
}

fun NavGraphBuilder.authGraph(navController: NavHostController) {
    val actions = NavActions(navController)
    navigation<AuthGraph>(startDestination = Onboarding) {
        composable<Onboarding> {
            Onboarding(
                onNavigateToLogin = actions::navigateToLoginReplacingOnboarding,
                onNavigateToRegister = actions::navigateToRegisterReplacingOnboarding
            )
        }
        composable<Login> {
            Login(onRegisterClick = actions::navigateToRegister)
        }
        composable<Register> {
            Register(onLoginClick = actions::navigateToLogin)
        }
    }
}
