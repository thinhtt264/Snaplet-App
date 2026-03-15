package com.thinh.snaplet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.thinh.snaplet.R

/**
 * Momo Trust Sans – default typeface for the app.
 * Font files: res/font/momo_trust_sans_*.ttf
 * See: https://github.com/typeassociates/MomoTrustSans
 */
val MomoTrustSans = FontFamily(
    Font(R.font.momo_trust_sans_extra_light, FontWeight.ExtraLight),
    Font(R.font.momo_trust_sans_light, FontWeight.Light),
    Font(R.font.momo_trust_sans_regular, FontWeight.Normal),
    Font(R.font.momo_trust_sans_medium, FontWeight.Medium),
    Font(R.font.momo_trust_sans_semi_bold, FontWeight.SemiBold),
    Font(R.font.momo_trust_sans_bold, FontWeight.Bold),
    Font(R.font.momo_trust_sans_extra_bold, FontWeight.ExtraBold),
)

val AppFontFamily: FontFamily = MomoTrustSans

private val NoPaddingTextStyle = PlatformTextStyle(
    includeFontPadding = false
)

private fun baseTextStyle(
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit = 0.sp
) = TextStyle(
    fontFamily = MomoTrustSans,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    platformStyle = NoPaddingTextStyle
)

/**
 * Material 3 Typography using Momo Trust Sans.
 * Applied via SnapletTheme; use MaterialTheme.typography in composable.
 */
val Typography = Typography(
    headlineLarge = baseTextStyle(FontWeight.ExtraBold, 28.sp, 40.sp),
    headlineMedium = baseTextStyle(FontWeight.ExtraBold, 24.sp, 36.sp),
    headlineSmall = baseTextStyle(FontWeight.ExtraBold, 22.sp, 32.sp),

    titleLarge = baseTextStyle(FontWeight.Bold, 20.sp, 28.sp),
    titleMedium = baseTextStyle(FontWeight.Bold, 18.sp, 24.sp, 0.15.sp),
    titleSmall = baseTextStyle(FontWeight.SemiBold, 16.sp, 20.sp, 0.1.sp),

    bodyLarge = baseTextStyle(FontWeight.SemiBold, 18.sp, 24.sp, 0.5.sp),
    bodyMedium = baseTextStyle(FontWeight.SemiBold, 16.sp, 20.sp, 0.25.sp),
    bodySmall = baseTextStyle(FontWeight.Medium, 14.sp, 16.sp, 0.4.sp),

    labelLarge = baseTextStyle(FontWeight.SemiBold, 14.sp, 20.sp, 0.1.sp),
    labelMedium = baseTextStyle(FontWeight.SemiBold, 12.sp, 16.sp, 0.5.sp),
    labelSmall = baseTextStyle(FontWeight.SemiBold, 11.sp, 16.sp, 0.5.sp),
)