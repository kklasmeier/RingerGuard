package com.ringerguard.app

object RingerGuardPrefs {
    const val PREFS_NAME = "ringer_guard_prefs"

    const val SILENCE_MODE = "silence_mode"
    const val GRACE_UNTIL_MS = "grace_until_ms"
    const val GRACE_DURATION_MIN = "grace_duration_min"
    const val RESTORE_VOLUME_LEVEL = "restore_volume_level"
    const val RESTORE_PRESERVE_VOLUME = "restore_preserve_volume"
    const val VOLUME_BEFORE_SILENCE = "volume_before_silence"
    const val CHECK_INTERVAL_MIN = "check_interval_min"
    const val RESTORE_DISABLE_DND = "restore_disable_dnd"
    const val RESTORE_DISABLE_VIBRATE = "restore_disable_vibrate"
    const val GRACE_ON_VOLUME_CHANGE = "grace_on_volume_change"
    const val GRACE_ON_CALL_DISMISS = "grace_on_call_dismiss"
    const val CALL_DISMISS_ENABLED = "call_dismiss_enabled"
    const val QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    const val QUIET_START_HOUR = "quiet_start_hour"
    const val QUIET_START_MIN = "quiet_start_min"
    const val QUIET_END_HOUR = "quiet_end_hour"
    const val QUIET_END_MIN = "quiet_end_min"
    const val QUIET_DAYS = "quiet_days"
    const val LAST_RESTORED_MS = "last_restored_ms"
    const val SERVICE_START_MS = "service_start_ms"
    const val PHONE_DISCLOSURE_ACK_MS = "phone_disclosure_ack_ms"
    const val IS_RESTORING_VOLUME = "is_restoring_volume"

    const val SILENCE_NONE = "none"
    const val SILENCE_TIMED = "timed"
    const val SILENCE_INDEFINITE = "indefinite"

    const val ACTION_CHECK = "com.ringerguard.app.action.CHECK"
    const val ACTION_WIDGET_TICK = "com.ringerguard.app.action.WIDGET_TICK"
    const val ACTION_SILENCE_TIMED = "com.ringerguard.app.action.SILENCE_TIMED"
    const val ACTION_SILENCE_INDEFINITE = "com.ringerguard.app.action.SILENCE_INDEFINITE"
    const val ACTION_RING_NOW = "com.ringerguard.app.action.RING_NOW"

    const val NOTIFICATION_ID = 1001
    const val CHANNEL_ID = "ringer_guard_channel"

    val GRACE_OPTIONS_MIN = intArrayOf(15, 30, 60, 120, 240)
}
