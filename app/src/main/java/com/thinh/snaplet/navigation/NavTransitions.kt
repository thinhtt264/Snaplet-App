package com.thinh.snaplet.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation.NavBackStackEntry

private const val NAV_ANIM_DURATION = 250
private const val FADE_DURATION_DIVISOR = 1
private const val ENTER_OFFSET_PERCENT = 0.3f
private const val EXIT_OFFSET_PERCENT = 0.15f

object NavTransitions {

    /** Default: slide from right on enter, slide to left on exit; reverse for pop. */
    object Default {
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * EXIT_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * EXIT_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    /** Home graph: fade + scale (zoom-style). */
    object HomeGraph {
        private const val ENTER_DURATION = 120
        private const val EXIT_DURATION = 90
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(tween(ENTER_DURATION)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(ENTER_DURATION, easing = FastOutSlowInEasing)
            )
        }
        val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(tween(EXIT_DURATION)) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(EXIT_DURATION)
            )
        }
    }

    /** MyProfile: slide in from left, slide out to left on pop. */
    object MyProfile {
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * EXIT_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = NAV_ANIM_DURATION / FADE_DURATION_DIVISOR,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
}
