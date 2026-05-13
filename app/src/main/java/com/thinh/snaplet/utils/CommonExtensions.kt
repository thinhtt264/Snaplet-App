package com.thinh.snaplet.utils

import com.thinh.snaplet.data.local.entity.MessageEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SAME_YEAR_DATE_LABEL_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d 'tháng' M")
private val OTHER_YEAR_DATE_LABEL_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d/M/yyyy")

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
    return this.toInstant().atZone(ZoneId.systemDefault()).format(TIME_FORMATTER)
}

fun <T : Comparable<T>> isGreaterWithFallback(a: T?, b: T?, fallback: Boolean): Boolean {
    return if (a != null && b != null) a >= b else fallback
}

fun MessageEntity.effectiveDate(myUserId: String?): Date {
    return if (myUserId != null && senderId == myUserId) {
        createdAt
    } else {
        serverCreatedAt ?: createdAt
    }
}

fun Date.toStartOfDayMillis(): Long {
    return toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

fun Long.toDateLabel(now: LocalDate = LocalDate.now()): String {
    val date = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (date.year == now.year) {
        date.format(SAME_YEAR_DATE_LABEL_FORMATTER)
    } else {
        date.format(OTHER_YEAR_DATE_LABEL_FORMATTER)
    }
}