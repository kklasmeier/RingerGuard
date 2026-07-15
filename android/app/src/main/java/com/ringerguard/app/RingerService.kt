package com.ringerguard.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class RingerService : Service() {
    private val volumeReceiver = VolumeChangeReceiver()
    private val callStateReceiver = CallStateReceiver()
    private var receiversRegistered = false

    override fun onCreate() {
        super.onCreate()
        val prefs = AppPreferences(this)
        if (prefs.serviceStartMs == 0L) {
            prefs.serviceStartMs = System.currentTimeMillis()
        }
        ensureReceiversRegistered()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(RingerGuardPrefs.NOTIFICATION_ID, NotificationHelper.build(this))
        updateCallReceiverRegistration()
        if (intent?.action == RingerGuardPrefs.ACTION_CHECK) {
            performCheck()
        } else {
            ServiceScheduler.scheduleImmediateCheck(this)
        }
        ServiceScheduler.scheduleWidgetTick(this)
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceivers()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun performCheck() {
        val prefs = AppPreferences(this)
        when (prefs.silenceMode) {
            RingerGuardPrefs.SILENCE_INDEFINITE -> Unit
            RingerGuardPrefs.SILENCE_TIMED -> {
                if (System.currentTimeMillis() >= prefs.graceUntilMs) {
                    prefs.silenceMode = RingerGuardPrefs.SILENCE_NONE
                    prefs.graceUntilMs = 0L
                    RingerRestoreHelper.restoreRinger(this, "Grace period expired")
                }
            }
            else -> {
                if (!QuietHoursHelper.isInQuietHours(this) && RingerRestoreHelper.needsRestore(this)) {
                    RingerRestoreHelper.restoreRinger(this, "Ringer was not at configured level")
                }
            }
        }
        updateForegroundNotification()
        ServiceScheduler.scheduleNextCheck(this)
    }

    private fun updateForegroundNotification() {
        NotificationManagerCompat.from(this).notify(
            RingerGuardPrefs.NOTIFICATION_ID,
            NotificationHelper.build(this),
        )
    }

    private fun ensureReceiversRegistered() {
        if (receiversRegistered) return
        registerReceiver(volumeReceiver, VolumeChangeReceiver.intentFilter())
        if (AppPreferences(this).callDismissEnabled) {
            registerCallStateReceiver()
        }
        receiversRegistered = true
    }

    fun registerCallStateReceiver() {
        if (!PermissionHelper.hasPhoneStatePermission(this)) return
        runCatching { unregisterReceiver(callStateReceiver) }
        registerReceiver(
            callStateReceiver,
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
        )
    }

    private fun updateCallReceiverRegistration() {
        if (AppPreferences(this).callDismissEnabled) {
            registerCallStateReceiver()
        }
    }

    private fun unregisterReceivers() {
        if (!receiversRegistered) return
        runCatching { unregisterReceiver(volumeReceiver) }
        runCatching { unregisterReceiver(callStateReceiver) }
        receiversRegistered = false
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = Intent(context, RingerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun refreshNotification(context: android.content.Context) {
            NotificationHelper.createChannel(context)
            NotificationManagerCompat.from(context).notify(
                RingerGuardPrefs.NOTIFICATION_ID,
                NotificationHelper.build(context),
            )
        }
    }
}

object NotificationHelper {
    fun createChannel(context: android.content.Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val channel = android.app.NotificationChannel(
            RingerGuardPrefs.CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            android.app.NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: android.content.Context): Notification {
        val prefs = AppPreferences(context)
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ringIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, WidgetActionReceiver::class.java).apply {
                action = RingerGuardPrefs.ACTION_RING_NOW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val (title, text) = when (prefs.silenceMode) {
            RingerGuardPrefs.SILENCE_TIMED -> {
                context.getString(
                    R.string.notification_silenced_title,
                    GracePeriodHelper.formatRemainingMinutes(prefs.graceUntilMs),
                ) to context.getString(R.string.notification_silenced_text)
            }
            RingerGuardPrefs.SILENCE_INDEFINITE -> {
                context.getString(R.string.notification_indefinite_title) to
                    context.getString(R.string.notification_indefinite_text)
            }
            else -> {
                val display = RingerDisplayHelper.resolve(context)
                display.badgeText to context.getString(R.string.notification_ringing_text)
            }
        }

        return NotificationCompat.Builder(context, RingerGuardPrefs.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .addAction(0, context.getString(R.string.action_ring_now), ringIntent)
            .build()
    }
}
