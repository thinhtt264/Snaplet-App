package com.thinh.snaplet.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.thinh.snaplet.ui.theme.MotionTokens

private const val ENTER_OFFSET_PERCENT = 0.1f
private const val EXIT_OFFSET_PERCENT = 0.15f

@SuppressLint("RestrictedApi")
private fun NavDestination.isNestedInAuthGraph(): Boolean {
    var current: NavDestination? = this
    while (current != null) {
        if (current.hasRoute(AuthGraph::class)) return true
        current = current.parent
    }
    return false
}

object NavTransitions {

    /** App-level transitions: handle special cases like app → AuthGraph. */
    object App {
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            if (targetState.destination.isNestedInAuthGraph()) {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = MotionTokens.Emphasized,
                        easing = FastOutSlowInEasing
                    )
                )
            } else {
                Default.enter(this)
            }
        }

        val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            if (targetState.destination.isNestedInAuthGraph()) {
                fadeOut(
                    animationSpec = tween(
                        durationMillis = MotionTokens.Emphasized,
                        easing = FastOutSlowInEasing
                    )
                )
            } else {
                ExitTransition.None
            }
        }
    }

    /** Default: slide from right on enter, slide to left on exit; reverse for pop. */
    object Default {
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = MotionTokens.Emphasized,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.Normal,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = MotionTokens.Emphasized,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = MotionTokens.Normal,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    /** MyProfile: slide in from left, slide out to left on pop. */
    object MyProfile {
        val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * ENTER_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = MotionTokens.Emphasized,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = MotionTokens.Normal,
                    easing = FastOutSlowInEasing
                )
            )
        }
        val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * EXIT_OFFSET_PERCENT).toInt() },
                animationSpec = tween(
                    durationMillis = MotionTokens.Emphasized,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = MotionTokens.Normal,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
}
