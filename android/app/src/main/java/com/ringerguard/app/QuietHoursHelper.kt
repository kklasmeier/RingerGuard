package com.ringerguard.app

import android.content.Context
import java.util.Calendar

object QuietHoursHelper {
    fun isInQuietHours(context: Context, now: Calendar = Calendar.getInstance()): Boolean {
        val prefs = AppPreferences(context)
        if (!prefs.quietHoursEnabled) return false

        val activeDays = prefs.quietDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek !in activeDays) return false

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = prefs.quietStartHour * 60 + prefs.quietStartMin
        val endMinutes = prefs.quietEndHour * 60 + prefs.quietEndMin

        return if (startMinutes <= endMinutes) {
            nowMinutes in startMinutes until endMinutes
        } else {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        }
    }

    fun formatTime(hour: Int, minute: Int): String {
        return "%02d:%02d".format(hour, minute)
    }
}
