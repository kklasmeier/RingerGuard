package com.ringerguard.app

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object SetupUiHelper {
    fun showBatteryExplanation(context: Context, onOpenSettings: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.setup_battery_dialog_title)
            .setMessage(R.string.setup_battery_dialog_body)
            .setPositiveButton(R.string.setup_open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showExactAlarmExplanation(context: Context, onOpenSettings: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.setup_exact_alarm_dialog_title)
            .setMessage(R.string.setup_exact_alarm_dialog_body)
            .setPositiveButton(R.string.setup_open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showDndExplanation(context: Context, onOpenSettings: () -> Unit, onSkip: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.setup_dnd_dialog_title)
            .setMessage(R.string.setup_dnd_dialog_body)
            .setPositiveButton(R.string.setup_open_settings) { _, _ -> onOpenSettings() }
            .setNegativeButton(R.string.setup_dnd_skip) { _, _ -> onSkip() }
            .show()
    }
}
