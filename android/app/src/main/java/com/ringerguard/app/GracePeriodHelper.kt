package com.ringerguard.app

import android.content.Context

object GracePeriodHelper {
    fun startGracePeriod(context: Context, triggerReason: String, durationMin: Int? = null) {
        val prefs = AppPreferences(context)
        val duration = durationMin ?: prefs.graceDurationMin
        val expiresAt = System.currentTimeMillis() + duration * 60_000L
        prefs.silenceMode = RingerGuardPrefs.SILENCE_TIMED
        prefs.graceUntilMs = expiresAt
        EventLogger.log(context, "Silenced — $triggerReason", "grace: ${duration}min", "silenced")
        RingerWidget.update(context)
        RingerService.refreshNotification(context)
    }

    fun setIndefinite(context: Context) {
        val prefs = AppPreferences(context)
        prefs.silenceMode = RingerGuardPrefs.SILENCE_INDEFINITE
        prefs.graceUntilMs = 0L
        EventLogger.log(context, "Silenced indefinitely", "manual", "silenced")
        RingerWidget.update(context)
        RingerService.refreshNotification(context)
    }

    fun ringNow(context: Context, reason: String = "manual") {
        val prefs = AppPreferences(context)
        prefs.silenceMode = RingerGuardPrefs.SILENCE_NONE
        prefs.graceUntilMs = 0L
        RingerRestoreHelper.restoreRinger(context, reason)
        RingerWidget.update(context)
        RingerService.refreshNotification(context)
    }

    fun formatRemaining(graceUntilMs: Long): String {
        val remainingMs = (graceUntilMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val totalSeconds = remainingMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun formatRemainingMinutes(graceUntilMs: Long): String {
        val remainingMin = ((graceUntilMs - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L).toInt()
        return "$remainingMin min"
    }
}
