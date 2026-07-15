package com.ringerguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.telephony.TelephonyManager

class VolumeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            VOLUME_CHANGED_ACTION -> intent?.let { handleVolumeChange(context, it) }
            RINGER_MODE_CHANGED_ACTION -> handleRingerModeChange(context)
        }
    }

    private fun handleVolumeChange(context: Context, intent: Intent) {
        val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
        if (streamType != AudioManager.STREAM_RING) return

        val prefs = AppPreferences(context)
        if (!prefs.isRestoringVolume) {
            val newVolume = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1)
            val oldVolume = intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, newVolume)
            if (newVolume >= 0 && newVolume > oldVolume && prefs.silenceMode != RingerGuardPrefs.SILENCE_NONE) {
                GracePeriodHelper.cancelSilenceOnManualRestore(context, "volume raised manually")
                return
            }
            if (prefs.graceOnVolumeChange && newVolume >= 0 && newVolume < oldVolume &&
                prefs.silenceMode == RingerGuardPrefs.SILENCE_NONE
            ) {
                GracePeriodHelper.startGracePeriod(context, "volume lowered")
            }
        }

        RingerDisplayHelper.refreshAllSurfaces(context)
    }

    private fun handleRingerModeChange(context: Context) {
        val prefs = AppPreferences(context)
        if (!prefs.isRestoringVolume) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                prefs.silenceMode != RingerGuardPrefs.SILENCE_NONE
            ) {
                GracePeriodHelper.cancelSilenceOnManualRestore(context, "ringer enabled manually")
                return
            }
            if (prefs.graceOnVolumeChange) {
                @Suppress("DEPRECATION")
                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_VIBRATE,
                    AudioManager.RINGER_MODE_SILENT,
                    -> {
                        if (prefs.silenceMode == RingerGuardPrefs.SILENCE_NONE) {
                            GracePeriodHelper.startGracePeriod(context, "ringer silenced")
                        }
                    }
                }
            }
        }

        RingerDisplayHelper.refreshAllSurfaces(context)
    }

    companion object {
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        const val RINGER_MODE_CHANGED_ACTION = "android.media.RINGER_MODE_CHANGED"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        private const val EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"

        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(VOLUME_CHANGED_ACTION)
            addAction(RINGER_MODE_CHANGED_ACTION)
        }
    }
}

class CallStateReceiver : BroadcastReceiver() {
    private var lastState = TelephonyManager.CALL_STATE_IDLE

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val prefs = AppPreferences(context)
        if (!prefs.callDismissEnabled || !prefs.graceOnCallDismiss) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)?.let { extra ->
            when (extra) {
                TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                else -> TelephonyManager.CALL_STATE_IDLE
            }
        } ?: TelephonyManager.CALL_STATE_IDLE

        if (lastState == TelephonyManager.CALL_STATE_RINGING && state == TelephonyManager.CALL_STATE_IDLE) {
            GracePeriodHelper.startGracePeriod(context, "call dismissed")
        }
        lastState = state
    }
}
