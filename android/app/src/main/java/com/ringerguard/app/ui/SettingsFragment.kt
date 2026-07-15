package com.ringerguard.app.ui

import android.Manifest
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ringerguard.app.AppPreferences
import com.ringerguard.app.BuildConfig
import com.ringerguard.app.DeviceHelper
import com.ringerguard.app.PermissionHelper
import com.ringerguard.app.R
import com.ringerguard.app.RingerGuardPrefs
import com.ringerguard.app.RingerService
import com.ringerguard.app.ServiceScheduler
import com.ringerguard.app.SetupUiHelper
import com.ringerguard.app.databinding.FragmentSettingsBinding
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phonePermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (_binding == null) return@registerForActivityResult
            val prefs = AppPreferences(requireContext())
            prefs.callDismissEnabled = granted
            if (granted) {
                RingerService.start(requireContext())
            } else {
                binding.switchGraceCall.isChecked = false
                prefs.graceOnCallDismiss = false
            }
            refreshHealth()
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        val context = requireContext()
        val prefs = AppPreferences(context)
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

        binding.sliderGrace.valueFrom = 0f
        binding.sliderGrace.valueTo = (RingerGuardPrefs.GRACE_OPTIONS_MIN.lastIndex).toFloat()
        binding.sliderGrace.stepSize = 1f
        binding.sliderGrace.value = RingerGuardPrefs.GRACE_OPTIONS_MIN.indexOf(prefs.graceDurationMin)
            .takeIf { it >= 0 }?.toFloat() ?: 2f
        binding.graceValue.text = "${prefs.graceDurationMin} minutes"
        binding.sliderGrace.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = RingerGuardPrefs.GRACE_OPTIONS_MIN[value.toInt()]
                prefs.graceDurationMin = minutes
                binding.graceValue.text = "$minutes minutes"
            }
        }

        binding.sliderVolume.valueFrom = 1f
        binding.sliderVolume.valueTo = maxVolume.toFloat()
        binding.sliderVolume.value = prefs.restoreVolumeLevel.coerceIn(1, maxVolume).toFloat()
        binding.volumeValue.text = "${prefs.restoreVolumeLevel.coerceIn(1, maxVolume)} / $maxVolume"
        binding.sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.restoreVolumeLevel = value.toInt()
                binding.volumeValue.text = "${value.toInt()} / $maxVolume"
            }
        }

        binding.switchRestorePreserveVolume.isChecked = prefs.restorePreserveVolume
        updateRestoreVolumeControls(prefs.restorePreserveVolume, maxVolume)
        binding.switchRestorePreserveVolume.setOnCheckedChangeListener { _, checked ->
            prefs.restorePreserveVolume = checked
            updateRestoreVolumeControls(checked, maxVolume)
        }

        binding.toggleCheckInterval.check(
            when (prefs.checkIntervalMin) {
                10 -> R.id.interval_10
                15 -> R.id.interval_15
                30 -> R.id.interval_30
                else -> R.id.interval_5
            },
        )
        binding.toggleCheckInterval.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            prefs.checkIntervalMin = when (checkedId) {
                R.id.interval_10 -> 10
                R.id.interval_15 -> 15
                R.id.interval_30 -> 30
                else -> 5
            }
            ServiceScheduler.scheduleImmediateCheck(requireContext())
        }

        binding.switchRestoreDnd.isChecked = prefs.restoreDisableDnd
        binding.switchRestoreVibrate.isChecked = prefs.restoreDisableVibrate
        binding.switchGraceVolume.isChecked = prefs.graceOnVolumeChange
        binding.switchGraceCall.isChecked = prefs.graceOnCallDismiss && prefs.callDismissEnabled

        binding.switchRestoreDnd.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                SetupUiHelper.showDndExplanation(
                    context,
                    onOpenSettings = {
                        prefs.restoreDisableDnd = true
                        PermissionHelper.requestDndAccess(context)
                    },
                    onSkip = {
                        binding.switchRestoreDnd.isChecked = false
                        prefs.restoreDisableDnd = false
                    },
                )
            } else {
                prefs.restoreDisableDnd = false
            }
        }
        binding.switchRestoreVibrate.setOnCheckedChangeListener { _, checked ->
            prefs.restoreDisableVibrate = checked
        }
        binding.switchGraceVolume.setOnCheckedChangeListener { _, checked ->
            prefs.graceOnVolumeChange = checked
        }
        binding.switchGraceCall.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                requestCallDismissFeature()
            } else {
                prefs.graceOnCallDismiss = false
                prefs.callDismissEnabled = false
            }
        }

        binding.aboutDeveloper.text = getString(R.string.developer_name)
        binding.aboutVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        refreshHealth()
    }

    override fun onResume() {
        super.onResume()
        refreshHealth()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun updateRestoreVolumeControls(preserveVolume: Boolean, maxVolume: Int) {
        binding.sliderVolume.isEnabled = !preserveVolume
        binding.volumeValue.alpha = if (preserveVolume) 0.5f else 1f
        if (preserveVolume) {
            binding.volumeValue.text = getString(R.string.settings_restore_preserve_volume)
        } else {
            val prefs = AppPreferences(requireContext())
            binding.volumeValue.text = "${prefs.restoreVolumeLevel.coerceIn(1, maxVolume)} / $maxVolume"
        }
    }

    private fun requestCallDismissFeature() {
        val prefs = AppPreferences(requireContext())
        if (PermissionHelper.hasPhoneStatePermission(requireContext())) {
            prefs.graceOnCallDismiss = true
            prefs.callDismissEnabled = true
            RingerService.start(requireContext())
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.phone_disclosure_title)
            .setMessage(R.string.phone_disclosure_body)
            .setPositiveButton(R.string.phone_disclosure_continue) { _, _ ->
                prefs.phoneDisclosureAckMs = System.currentTimeMillis()
                prefs.graceOnCallDismiss = true
                phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            }
            .setNegativeButton(R.string.phone_disclosure_cancel) { _, _ ->
                binding.switchGraceCall.isChecked = false
                prefs.graceOnCallDismiss = false
            }
            .show()
    }

    private fun refreshHealth() {
        val context = requireContext()
        val prefs = AppPreferences(context)
        binding.healthBattery.text = "${getString(R.string.settings_battery)}: " +
            if (PermissionHelper.hasBatteryExemption(context)) getString(R.string.settings_active) else getString(R.string.settings_inactive)
        binding.healthDnd.text = "${getString(R.string.settings_dnd_access)}: " +
            if (PermissionHelper.hasDndAccess(context)) getString(R.string.settings_granted) else getString(R.string.settings_not_granted)
        binding.healthExactAlarm.text = "${getString(R.string.settings_exact_alarm)}: " +
            if (PermissionHelper.canScheduleExactAlarms(context)) getString(R.string.settings_granted) else getString(R.string.settings_not_granted)
        binding.healthPhone.text = "${getString(R.string.settings_phone_state)}: " +
            when {
                PermissionHelper.hasPhoneStatePermission(context) -> getString(R.string.settings_granted)
                prefs.graceOnCallDismiss -> getString(R.string.settings_not_granted)
                else -> getString(R.string.settings_never)
            }

        val uptimeMs = System.currentTimeMillis() - prefs.serviceStartMs.coerceAtLeast(0L)
        val hours = TimeUnit.MILLISECONDS.toHours(uptimeMs)
        binding.healthUptime.text = "${getString(R.string.settings_uptime)}: ${hours}h"
        binding.healthLastRestore.text = "${getString(R.string.settings_last_restore)}: " +
            if (prefs.lastRestoredMs > 0) {
                com.ringerguard.app.EventLogger.formatTime(prefs.lastRestoredMs)
            } else {
                getString(R.string.settings_never)
            }

        if (PermissionHelper.hasBatteryExemption(context)) {
            binding.settingsBatteryTip.visibility = View.GONE
        } else {
            binding.settingsBatteryTip.visibility = View.VISIBLE
            binding.settingsBatteryTip.text = if (DeviceHelper.isSamsung()) {
                getString(R.string.settings_samsung_battery_tip)
            } else {
                getString(R.string.settings_generic_battery_tip)
            }
        }
    }
}
