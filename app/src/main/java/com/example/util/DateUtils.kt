package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatRelativeTime(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMillis

        if (diff < 60_000L) return "Just now"
        if (diff < 3_600_000L) return "${diff / 60_000L}m ago"

        val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isToday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isToday) {
            return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))
        }

        val yesterdayCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        if (diff < 7 * 86_400_000L) {
            return SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestampMillis))
        }

        return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMillis))
    }

    fun formatMessageTime(timestampMillis: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))
    }

    fun formatDateHeader(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isToday = msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            msgCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
        if (isToday) return "Today"

        val yesterdayCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Yesterday"

        return SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestampMillis))
    }

    fun formatDurationSeconds(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
    }
}
