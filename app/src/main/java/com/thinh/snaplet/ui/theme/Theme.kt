package com.thinh.snaplet.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = primary_dark,
    onPrimary = onPrimary_dark,
    primaryContainer = primaryContainer_dark,
    onPrimaryContainer = onPrimaryContainer_dark,
    inversePrimary = inversePrimary_dark,
    primaryFixed = primaryFixed_dark,
    primaryFixedDim = primaryFixedDim_dark,
    onPrimaryFixed = onPrimaryFixed_dark,
    onPrimaryFixedVariant = onPrimaryFixedVariant_dark,
    secondary = secondary_dark,
    onSecondary = onSecondary_dark,
    secondaryContainer = secondaryContainer_dark,
    onSecondaryContainer = onSecondaryContainer_dark,
    secondaryFixed = secondaryFixed_dark,
    secondaryFixedDim = secondaryFixedDim_dark,
    onSecondaryFixed = onSecondaryFixed_dark,
    onSecondaryFixedVariant = onSecondaryFixedVariant_dark,
    tertiary = tertiary_dark,
    onTertiary = onTertiary_dark,
    tertiaryContainer = tertiaryContainer_dark,
    onTertiaryContainer = onTertiaryContainer_dark,
    tertiaryFixed = tertiaryFixed_dark,
    tertiaryFixedDim = tertiaryFixedDim_dark,
    onTertiaryFixed = onTertiaryFixed_dark,
    onTertiaryFixedVariant = onTertiaryFixedVariant_dark,
    error = error_dark,
    onError = onError_dark,
    errorContainer = errorContainer_dark,
    onErrorContainer = onErrorContainer_dark,
    background = background_dark,
    onBackground = onBackground_dark,
    surface = surface_dark,
    onSurface = onSurface_dark,
    surfaceVariant = surfaceVariant_dark,
    onSurfaceVariant = onSurfaceVariant_dark,
    surfaceTint = surfaceTint_dark,
    surfaceDim = surfaceDim_dark,
    surfaceBright = surfaceBright_dark,
    surfaceContainerLowest = surfaceContainerLowest_dark,
    surfaceContainerLow = surfaceContainerLow_dark,
    surfaceContainer = surfaceContainer_dark,
    surfaceContainerHigh = surfaceContainerHigh_dark,
    surfaceContainerHighest = surfaceContainerHighest_dark,
    inverseSurface = inverseSurface_dark,
    inverseOnSurface = inverseOnSurface_dark,
    outline = outline_dark,
    outlineVariant = outlineVariant_dark,
)

private val LightColorScheme = lightColorScheme(
    primary = primary_light,
    onPrimary = onPrimary_light,
    primaryContainer = primaryContainer_light,
    onPrimaryContainer = onPrimaryContainer_light,
    inversePrimary = inversePrimary_light,
    primaryFixed = primaryFixed_light,
    primaryFixedDim = primaryFixedDim_light,
    onPrimaryFixed = onPrimaryFixed_light,
    onPrimaryFixedVariant = onPrimaryFixedVariant_light,
    secondary = secondary_light,
    onSecondary = onSecondary_light,
    secondaryContainer = secondaryContainer_light,
    onSecondaryContainer = onSecondaryContainer_light,
    secondaryFixed = secondaryFixed_light,
    secondaryFixedDim = secondaryFixedDim_light,
    onSecondaryFixed = onSecondaryFixed_light,
    onSecondaryFixedVariant = onSecondaryFixedVariant_light,
    tertiary = tertiary_light,
    onTertiary = onTertiary_light,
    tertiaryContainer = tertiaryContainer_light,
    onTertiaryContainer = onTertiaryContainer_light,
    tertiaryFixed = tertiaryFixed_light,
    tertiaryFixedDim = tertiaryFixedDim_light,
    onTertiaryFixed = onTertiaryFixed_light,
    onTertiaryFixedVariant = onTertiaryFixedVariant_light,
    error = error_light,
    onError = onError_light,
    errorContainer = errorContainer_light,
    onErrorContainer = onErrorContainer_light,
    background = background_light,
    onBackground = onBackground_light,
    surface = surface_light,
    onSurface = onSurface_light,
    surfaceVariant = surfaceVariant_light,
    onSurfaceVariant = onSurfaceVariant_light,
    surfaceTint = surfaceTint_light,
    surfaceDim = surfaceDim_light,
    surfaceBright = surfaceBright_light,
    surfaceContainerLowest = surfaceContainerLowest_light,
    surfaceContainerLow = surfaceContainerLow_light,
    surfaceContainer = surfaceContainer_light,
    surfaceContainerHigh = surfaceContainerHigh_light,
    surfaceContainerHighest = surfaceContainerHighest_light,
    inverseSurface = inverseSurface_light,
    inverseOnSurface = inverseOnSurface_light,
    outline = outline_light,
    outlineVariant = outlineVariant_light,
)

@Composable
fun SnapletTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? androidx.activity.ComponentActivity)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)

                WindowInsetsControllerCompat(it, it.decorView).apply {
                    isAppearanceLightStatusBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}