package com.thinh.snaplet.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Backwards-compatible helper used in existing call sites.
 * Delegates to [Date.toLocalTimeAgo].
 */
fun formatTimeAgo(createdAt: Date): String = createdAt.toLocalTimeAgo()

/**
 * Convert a UTC-backed [Date] to a relative "time ago" string
 * using the device's current time as reference.
 */
fun Date.toLocalTimeAgo(): String {
    return try {
        val createdTime = time
        val now = System.currentTimeMillis()
        val diff = kotlin.math.abs(now - createdTime)

        when {
            diff < 60_000 -> "${diff / 1_000}s" // seconds
            diff < 3_600_000 -> "${diff / 60_000}m" // minutes
            diff < 86_400_000 -> "${diff / 3_600_000}h" // hours
            diff < 604_800_000 -> "${diff / 86_400_000}d" // days
            diff < 2_592_000_000L -> "${diff / 604_800_000}w" // weeks
            diff < 31_536_000_000L -> "${diff / 2_592_000_000L}mo" // months
            else -> "${diff / 31_536_000_000L}y" // years
        }
    } catch (_: Exception) {
        ""
    }
}

/**
 * Format a UTC-backed [Date] into a human-readable local time string
 * using the device's timezone.
 *
 * Default format example: "14:32 · 18/03/2026"
 */
fun Date.toLocalDisplay(pattern: String = "HH:mm · dd/MM/yyyy"): String {
    return try {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        formatter.format(this)
    } catch (_: Exception) {
        ""
    }
}
