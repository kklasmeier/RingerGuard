package com.ringerguard.app

import android.content.Context

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(RingerGuardPrefs.PREFS_NAME, Context.MODE_PRIVATE)

    var silenceMode: String
        get() = prefs.getString(RingerGuardPrefs.SILENCE_MODE, RingerGuardPrefs.SILENCE_NONE)
            ?: RingerGuardPrefs.SILENCE_NONE
        set(value) = prefs.edit().putString(RingerGuardPrefs.SILENCE_MODE, value).apply()

    var graceUntilMs: Long
        get() = prefs.getLong(RingerGuardPrefs.GRACE_UNTIL_MS, 0L)
        set(value) = prefs.edit().putLong(RingerGuardPrefs.GRACE_UNTIL_MS, value).apply()

    var graceDurationMin: Int
        get() = prefs.getInt(RingerGuardPrefs.GRACE_DURATION_MIN, 60)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.GRACE_DURATION_MIN, value).apply()

    var restoreVolumeLevel: Int
        get() = prefs.getInt(RingerGuardPrefs.RESTORE_VOLUME_LEVEL, -1)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.RESTORE_VOLUME_LEVEL, value).apply()

    var restorePreserveVolume: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.RESTORE_PRESERVE_VOLUME, false)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.RESTORE_PRESERVE_VOLUME, value).apply()

    var volumeBeforeSilence: Int
        get() = prefs.getInt(RingerGuardPrefs.VOLUME_BEFORE_SILENCE, -1)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.VOLUME_BEFORE_SILENCE, value).apply()

    var checkIntervalMin: Int
        get() = prefs.getInt(RingerGuardPrefs.CHECK_INTERVAL_MIN, 5)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.CHECK_INTERVAL_MIN, value).apply()

    var restoreDisableDnd: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.RESTORE_DISABLE_DND, false)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.RESTORE_DISABLE_DND, value).apply()

    var restoreDisableVibrate: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.RESTORE_DISABLE_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.RESTORE_DISABLE_VIBRATE, value).apply()

    var graceOnVolumeChange: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.GRACE_ON_VOLUME_CHANGE, true)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.GRACE_ON_VOLUME_CHANGE, value).apply()

    var graceOnCallDismiss: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.GRACE_ON_CALL_DISMISS, true)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.GRACE_ON_CALL_DISMISS, value).apply()

    var callDismissEnabled: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.CALL_DISMISS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.CALL_DISMISS_ENABLED, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.QUIET_HOURS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.QUIET_HOURS_ENABLED, value).apply()

    var quietStartHour: Int
        get() = prefs.getInt(RingerGuardPrefs.QUIET_START_HOUR, 22)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.QUIET_START_HOUR, value).apply()

    var quietStartMin: Int
        get() = prefs.getInt(RingerGuardPrefs.QUIET_START_MIN, 0)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.QUIET_START_MIN, value).apply()

    var quietEndHour: Int
        get() = prefs.getInt(RingerGuardPrefs.QUIET_END_HOUR, 7)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.QUIET_END_HOUR, value).apply()

    var quietEndMin: Int
        get() = prefs.getInt(RingerGuardPrefs.QUIET_END_MIN, 0)
        set(value) = prefs.edit().putInt(RingerGuardPrefs.QUIET_END_MIN, value).apply()

    var quietDays: String
        get() = prefs.getString(RingerGuardPrefs.QUIET_DAYS, "2,3,4,5,6") ?: "2,3,4,5,6"
        set(value) = prefs.edit().putString(RingerGuardPrefs.QUIET_DAYS, value).apply()

    var lastRestoredMs: Long
        get() = prefs.getLong(RingerGuardPrefs.LAST_RESTORED_MS, 0L)
        set(value) = prefs.edit().putLong(RingerGuardPrefs.LAST_RESTORED_MS, value).apply()

    var serviceStartMs: Long
        get() = prefs.getLong(RingerGuardPrefs.SERVICE_START_MS, 0L)
        set(value) = prefs.edit().putLong(RingerGuardPrefs.SERVICE_START_MS, value).apply()

    var phoneDisclosureAckMs: Long
        get() = prefs.getLong(RingerGuardPrefs.PHONE_DISCLOSURE_ACK_MS, 0L)
        set(value) = prefs.edit().putLong(RingerGuardPrefs.PHONE_DISCLOSURE_ACK_MS, value).apply()

    var isRestoringVolume: Boolean
        get() = prefs.getBoolean(RingerGuardPrefs.IS_RESTORING_VOLUME, false)
        set(value) = prefs.edit().putBoolean(RingerGuardPrefs.IS_RESTORING_VOLUME, value).apply()
}
