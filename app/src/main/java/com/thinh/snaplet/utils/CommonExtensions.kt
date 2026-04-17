package com.thinh.snaplet.utils

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private val TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

fun formatTimeAgo(createdAt: Date): String = createdAt.toLocalTimeAgo()

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

fun Date.to24HourTime(): String {
    return this.toInstant()
        .atZone(ZoneId.systemDefault())
        .format(TIME_FORMATTER)
}