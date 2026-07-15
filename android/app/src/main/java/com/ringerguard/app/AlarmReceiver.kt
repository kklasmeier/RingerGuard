package com.ringerguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "RingerGuard:AlarmWakeLock",
        )
        wakeLock.acquire(10_000L)
        try {
            when (action) {
                RingerGuardPrefs.ACTION_CHECK -> {
                    val serviceIntent = Intent(context, RingerService::class.java).apply {
                        this.action = RingerGuardPrefs.ACTION_CHECK
                    }
                    context.startForegroundService(serviceIntent)
                }
                RingerGuardPrefs.ACTION_WIDGET_TICK -> {
                    RingerWidget.update(context)
                    ServiceScheduler.scheduleWidgetTick(context)
                }
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}
