package com.motomusic.app.core

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Formats a duration as `m:ss`, or `h:mm:ss` for anything over an hour. */
fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Human readable file size, e.g. `4.8 MB`. */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Unknown"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "${bytes} ${units[unit]}" else String.format(Locale.US, "%.1f %s", value, units[unit])
}

/** `320 kbps`, or null when the source did not report a bitrate. */
fun formatBitrate(bitsPerSecond: Int?): String? {
    if (bitsPerSecond == null || bitsPerSecond <= 0) return null
    return "${bitsPerSecond / 1000} kbps"
}

fun formatSampleRate(hertz: Int?): String? {
    if (hertz == null || hertz <= 0) return null
    return String.format(Locale.US, "%.1f kHz", hertz / 1000.0)
}

fun pluralise(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"

/** Total listening time in words, e.g. `48 min` or `1 hr 12 min`. */
fun formatTotalDuration(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    if (totalMinutes < 1) return "under a minute"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "$totalMinutes min"
        minutes == 0L -> pluralise(hours.toInt(), "hr")
        else -> "${pluralise(hours.toInt(), "hr")} $minutes min"
    }
}
