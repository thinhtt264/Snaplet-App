package com.thinh.snaplet.utils

import android.os.SystemClock
import com.thinh.snaplet.ui.theme.MotionTokens
import kotlinx.coroutines.delay

private const val MIN_LOADING_TIME_MS = MotionTokens.VerySlow.toLong()

suspend fun ensureMinLoadingTime(
    startTimeMillis: Long,
    minDurationMillis: Long = MIN_LOADING_TIME_MS,
) {
    val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
    val remaining = minDurationMillis - elapsed
    if (remaining > 0) delay(remaining)
}