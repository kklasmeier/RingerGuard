package com.ringerguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            RingerGuardPrefs.ACTION_SILENCE_TIMED -> {
                GracePeriodHelper.startGracePeriod(context, "widget")
            }
            RingerGuardPrefs.ACTION_SILENCE_INDEFINITE -> {
                GracePeriodHelper.setIndefinite(context)
            }
            RingerGuardPrefs.ACTION_RING_NOW -> {
                GracePeriodHelper.ringNow(context)
            }
        }
    }
}
