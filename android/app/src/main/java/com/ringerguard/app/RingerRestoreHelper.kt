package com.ringerguard.app

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.ringerguard.app.BuildConfig

object RingerRestoreHelper {
    fun restoreRinger(context: Context, detail: String = "scheduled check") {
        val prefs = AppPreferences(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val targetVolume = if (prefs.restoreVolumeLevel <= 0) {
            maxVolume
        } else {
            prefs.restoreVolumeLevel.coerceIn(1, maxVolume)
        }

        prefs.isRestoringVolume = true
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                targetVolume,
                0,
            )

            if (prefs.restoreDisableVibrate) {
                @Suppress("DEPRECATION")
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }

            if (prefs.restoreDisableDnd && PermissionHelper.hasDndAccess(context)) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } finally {
            prefs.isRestoringVolume = false
        }

        prefs.lastRestoredMs = System.currentTimeMillis()
        EventLogger.log(
            context,
            "Ringer restored to level $targetVolume",
            detail,
            if (detail == "manual") "manual" else "restored",
        )
        RingerWidget.update(context)
        RingerService.refreshNotification(context)
    }

    fun needsRestore(context: Context): Boolean {
        val prefs = AppPreferences(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val targetVolume = if (prefs.restoreVolumeLevel <= 0) maxVolume else prefs.restoreVolumeLevel.coerceIn(1, maxVolume)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        @Suppress("DEPRECATION")
        val vibrateOnly = audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
            audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT

        val dndActive = if (PermissionHelper.hasDndAccess(context)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }

        return currentVolume < targetVolume || (prefs.restoreDisableVibrate && vibrateOnly) ||
            (prefs.restoreDisableDnd && dndActive)
    }

    fun effectiveRestoreVolume(context: Context): Int {
        val prefs = AppPreferences(context)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        return if (prefs.restoreVolumeLevel <= 0) maxVolume else prefs.restoreVolumeLevel.coerceIn(1, maxVolume)
    }

    fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d("RingerGuard", message)
        }
    }
}
