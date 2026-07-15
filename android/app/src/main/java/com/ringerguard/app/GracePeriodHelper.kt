package com.ringerguard.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

object GracePeriodHelper {
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val USER_SILENCE_REASONS = setOf(
        "quick action",
        "custom duration",
        "widget",
    )

    fun startGracePeriod(context: Context, triggerReason: String, durationMin: Int? = null) {
        val prefs = AppPreferences(context)
        val duration = durationMin ?: prefs.graceDurationMin
        val expiresAt = System.currentTimeMillis() + duration * 60_000L
        prefs.silenceMode = RingerGuardPrefs.SILENCE_TIMED
        prefs.graceUntilMs = expiresAt

        if (triggerReason in USER_SILENCE_REASONS) {
            RingerRestoreHelper.silenceDevice(context)
        }

        scheduleSideEffects(context, "Silenced — $triggerReason", "grace: ${duration}min", "silenced")
    }

    fun setIndefinite(context: Context) {
        val prefs = AppPreferences(context)
        prefs.silenceMode = RingerGuardPrefs.SILENCE_INDEFINITE
        prefs.graceUntilMs = 0L
        RingerRestoreHelper.silenceDevice(context)
        scheduleSideEffects(context, "Silenced indefinitely", "manual", "silenced")
    }

    fun cancelSilenceOnManualRestore(context: Context, detail: String) {
        val prefs = AppPreferences(context)
        if (prefs.isRestoringVolume || prefs.silenceMode == RingerGuardPrefs.SILENCE_NONE) return

        prefs.silenceMode = RingerGuardPrefs.SILENCE_NONE
        prefs.graceUntilMs = 0L
        EventLogger.log(context, "Silence cancelled", detail, "manual")
        RingerDisplayHelper.refreshAllSurfaces(context)
    }

    fun ringNow(context: Context, reason: String = "manual") {
        val prefs = AppPreferences(context)
        prefs.silenceMode = RingerGuardPrefs.SILENCE_NONE
        prefs.graceUntilMs = 0L
        mainHandler.post {
            RingerRestoreHelper.restoreRinger(context, reason)
        }
    }

    private fun scheduleSideEffects(context: Context, title: String?, detail: String?, type: String?) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            if (title != null && detail != null && type != null) {
                EventLogger.log(appContext, title, detail, type)
            }
            mainHandler.post { RingerDisplayHelper.refreshAllSurfaces(appContext) }
        }
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
