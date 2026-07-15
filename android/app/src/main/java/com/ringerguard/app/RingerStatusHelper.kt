package com.ringerguard.app

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

object RingerStatusHelper {
    data class LiveStatus(
        val currentVolume: Int,
        val maxVolume: Int,
        val dndOn: Boolean,
        val vibrateOnly: Boolean,
        val serviceRunning: Boolean,
    )

    fun read(context: Context): LiveStatus {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        @Suppress("DEPRECATION")
        val ringerMode = audioManager.ringerMode
        val vibrateOnly = ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
            ringerMode == AudioManager.RINGER_MODE_SILENT

        val dndOn = if (PermissionHelper.hasDndAccess(context)) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } else {
            false
        }

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val serviceRunning = activityManager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == RingerService::class.java.name }

        return LiveStatus(currentVolume, maxVolume, dndOn, vibrateOnly, serviceRunning)
    }
}
