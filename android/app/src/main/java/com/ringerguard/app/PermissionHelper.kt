package com.ringerguard.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

enum class SetupIssue {
    BATTERY,
    EXACT_ALARM,
}

object PermissionHelper {
    fun hasBatteryExemption(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryExemption(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun requestDndAccess(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Required setup only — DND is optional and never included here. */
    fun firstSetupIssue(context: Context): SetupIssue? {
        if (!hasBatteryExemption(context)) return SetupIssue.BATTERY
        if (!canScheduleExactAlarms(context)) return SetupIssue.EXACT_ALARM
        return null
    }

    fun requiredSetupStep(context: Context): Pair<Int, Int>? {
        return when (firstSetupIssue(context)) {
            SetupIssue.BATTERY -> 1 to 2
            SetupIssue.EXACT_ALARM -> 2 to 2
            null -> null
        }
    }

    fun openSetupIssue(context: Context, issue: SetupIssue) {
        when (issue) {
            SetupIssue.BATTERY -> requestBatteryExemption(context)
            SetupIssue.EXACT_ALARM -> requestExactAlarmPermission(context)
        }
    }
}
