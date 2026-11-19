package com.tty.launcher.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private const val KB = 1024.0
    private const val MB = KB * 1024
    private const val GB = MB * 1024

    fun bytes(value: Long): String = when {
        value >= GB -> String.format(Locale.US, "%.1f GB", value / GB)
        value >= MB -> String.format(Locale.US, "%.1f MB", value / MB)
        value >= KB -> String.format(Locale.US, "%.0f KB", value / KB)
        else -> "$value B"
    }

    /** Terminal aligned duration, e.g. 04:32:18 (hours can exceed 24). */
    fun uptime(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun shortDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        return when {
            totalSeconds < 60 -> "${totalSeconds}s"
            totalSeconds < 3600 -> "${totalSeconds / 60}m ${totalSeconds % 60}s"
            else -> "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
        }
    }

    fun dateTime(epochMillis: Long, pattern: String): String = format(
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()),
        pattern
    )

    fun time(epochMillis: Long, pattern: String): String = dateTime(epochMillis, pattern)

    fun clockTime(epochMillis: Long): String =
        dateTime(epochMillis, "HH:mm") + ":" + dateTime(epochMillis, "ss")

    private fun format(value: LocalDateTime, pattern: String): String = try {
        value.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    } catch (e: IllegalArgumentException) {
        // Invalid user supplied pattern: fall back to ISO-ish output.
        value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US))
    }

    fun percent(part: Long, total: Long): Int =
        if (total <= 0L) 0 else ((part.toDouble() / total) * 100).toInt().coerceIn(0, 100)

    /** Pads "DEVICE : value" style rows so the terminal stays aligned. */
    fun row(label: String, value: String, width: Int = 11): String =
        label.uppercase(Locale.US).padEnd(width) + ": " + value
}
