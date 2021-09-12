package kz.q19.kenes.widget.util

import java.util.concurrent.TimeUnit

fun Long?.formatToDigitalClock(): String {
    if (this == null) return "00:00"
    return try {
        val hours = TimeUnit.MILLISECONDS.toHours(this) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(this) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
        when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            seconds > 0 -> String.format("00:%02d", seconds)
            else -> "00:00"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return "00:00"
    }
}