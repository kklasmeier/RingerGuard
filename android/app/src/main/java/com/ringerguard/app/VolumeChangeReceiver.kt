package com.ringerguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.telephony.TelephonyManager

class VolumeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != VOLUME_CHANGED_ACTION) return
        val prefs = AppPreferences(context)
        if (prefs.isRestoringVolume || !prefs.graceOnVolumeChange) return

        val streamType = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
        if (streamType != AudioManager.STREAM_RING) return

        val newVolume = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1)
        val oldVolume = intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, newVolume)
        if (newVolume >= 0 && newVolume < oldVolume) {
            GracePeriodHelper.startGracePeriod(context, "volume lowered")
        }
    }

    companion object {
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        private const val EXTRA_PREV_VOLUME_STREAM_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
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
