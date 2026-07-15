package com.ringerguard.app.ui

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ringerguard.app.AppPreferences
import com.ringerguard.app.BuildConfig
import com.ringerguard.app.GracePeriodHelper
import com.ringerguard.app.PermissionHelper
import com.ringerguard.app.R
import com.ringerguard.app.RingerDisplayHelper
import com.ringerguard.app.RingerGuardPrefs
import com.ringerguard.app.SetupIssue
import com.ringerguard.app.SetupUiHelper
import com.ringerguard.app.databinding.DialogCustomDurationBinding
import com.ringerguard.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home), ForegroundRefreshable {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        val prefs = AppPreferences(requireContext())

        binding.btnSilenceTimed.text = getString(R.string.action_silence_duration, prefs.graceDurationMin)
        binding.btnSilenceTimed.setOnClickListener {
            beginQuickAction()
            GracePeriodHelper.startGracePeriod(requireContext(), "quick action")
            applyDisplayModel()
            endQuickAction()
        }
        binding.btnSilenceIndefinite.setOnClickListener {
            beginQuickAction()
            GracePeriodHelper.setIndefinite(requireContext())
            applyDisplayModel()
            endQuickAction()
        }
        binding.btnRingNow.setOnClickListener {
            beginQuickAction()
            GracePeriodHelper.ringNow(requireContext())
            applyDisplayModel()
            endQuickAction()
        }
        binding.btnCustomDuration.setOnClickListener { showCustomDurationDialog() }
        binding.appVersionFooter.text = getString(R.string.app_version_footer, BuildConfig.VERSION_NAME)
        binding.setupBannerButton.setOnClickListener {
            when (PermissionHelper.firstSetupIssue(requireContext())) {
                SetupIssue.BATTERY -> SetupUiHelper.showBatteryExplanation(requireContext()) {
                    PermissionHelper.requestBatteryExemption(requireContext())
                }
                SetupIssue.EXACT_ALARM -> SetupUiHelper.showExactAlarmExplanation(requireContext()) {
                    PermissionHelper.requestExactAlarmPermission(requireContext())
                }
                null -> Unit
            }
        }
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onForegroundRefresh() {
        if (_binding == null) return
        applyDisplayModel()
    }

    private fun beginQuickAction() {
        setQuickActionsEnabled(false)
        binding.quickActionsProgress.visibility = View.VISIBLE
    }

    private fun endQuickAction() {
        binding.root.postDelayed({
            if (_binding != null) {
                refreshUi()
                setQuickActionsEnabled(true)
                binding.quickActionsProgress.visibility = View.GONE
            }
        }, 400L)
    }

    private fun setQuickActionsEnabled(enabled: Boolean) {
        binding.btnSilenceTimed.isEnabled = enabled
        binding.btnSilenceIndefinite.isEnabled = enabled
        binding.btnRingNow.isEnabled = enabled
        binding.btnCustomDuration.isEnabled = enabled
    }

    private fun applyDisplayModel() {
        val context = requireContext()
        val prefs = AppPreferences(context)
        val model = RingerDisplayHelper.resolve(context)
        binding.statusCard.setCardBackgroundColor(
            ContextCompat.getColor(context, model.backgroundColorRes),
        )
        binding.statusIcon.setImageResource(model.statusIconRes)
        binding.statusIcon.setColorFilter(
            ContextCompat.getColor(context, model.textColorRes),
            PorterDuff.Mode.SRC_IN,
        )
        binding.statusTitle.setTextColor(ContextCompat.getColor(context, model.textColorRes))
        if (prefs.silenceMode == RingerGuardPrefs.SILENCE_TIMED) {
            binding.statusTitle.text = getString(R.string.status_silenced_heading)
            binding.statusCountdown.visibility = View.VISIBLE
            binding.statusCountdown.text = GracePeriodHelper.formatRemaining(prefs.graceUntilMs)
            binding.statusCountdown.setTextColor(ContextCompat.getColor(context, model.textColorRes))
        } else {
            binding.statusTitle.text = model.title
            binding.statusCountdown.visibility = View.GONE
        }
    }

    private fun refreshUi() {
        val context = requireContext()
        val prefs = AppPreferences(context)
        binding.btnSilenceTimed.text = getString(R.string.action_silence_duration, prefs.graceDurationMin)
        applyDisplayModel()

        val live = com.ringerguard.app.RingerStatusHelper.read(context)
        binding.liveVolume.text = "${getString(R.string.live_volume)}: ${live.currentVolume} / ${live.maxVolume}"
        binding.liveDnd.text = "${getString(R.string.live_dnd)}: ${if (live.dndOn) getString(R.string.on) else getString(R.string.off)}"
        binding.liveVibrate.text = "${getString(R.string.live_vibrate)}: ${if (live.vibrateOnly) getString(R.string.on) else getString(R.string.off)}"
        binding.liveService.text = "${getString(R.string.live_service)}: ${if (live.serviceRunning) getString(R.string.yes) else getString(R.string.no)}"

        val issue = PermissionHelper.firstSetupIssue(context)
        if (issue == null) {
            binding.setupBanner.visibility = View.GONE
        } else {
            binding.setupBanner.visibility = View.VISIBLE
            PermissionHelper.requiredSetupStep(context)?.let { (step, total) ->
                binding.setupBannerStep.text = getString(R.string.setup_banner_step, step, total)
                binding.setupBannerStep.visibility = View.VISIBLE
            }
            binding.setupBannerText.text = when (issue) {
                SetupIssue.BATTERY -> getString(R.string.setup_banner_battery)
                SetupIssue.EXACT_ALARM -> getString(R.string.setup_banner_exact_alarm)
            }
        }
    }

    private fun showCustomDurationDialog() {
        val dialogBinding = DialogCustomDurationBinding.inflate(layoutInflater)
        val prefs = AppPreferences(requireContext())
        val initial = prefs.graceDurationMin.coerceIn(5, 240).let { value ->
            if (value % 5 == 0) value else value + (5 - value % 5)
        }.toFloat()
        dialogBinding.customDurationSlider.value = initial
        dialogBinding.customDurationValue.text = getString(R.string.custom_duration_label, initial.toInt())

        dialogBinding.customDurationSlider.addOnChangeListener { _, value, _ ->
            dialogBinding.customDurationValue.text = getString(R.string.custom_duration_label, value.toInt())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_custom_duration)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.ok) { _, _ ->
                val minutes = dialogBinding.customDurationSlider.value.toInt()
                beginQuickAction()
                GracePeriodHelper.startGracePeriod(requireContext(), "custom duration", minutes)
                applyDisplayModel()
                endQuickAction()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
