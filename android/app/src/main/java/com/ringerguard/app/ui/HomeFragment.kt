package com.ringerguard.app.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ringerguard.app.AppPreferences
import com.ringerguard.app.GracePeriodHelper
import com.ringerguard.app.PermissionHelper
import com.ringerguard.app.R
import com.ringerguard.app.RingerGuardPrefs
import com.ringerguard.app.RingerStatusHelper
import com.ringerguard.app.SetupIssue
import com.ringerguard.app.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (_binding != null) {
                refreshUi()
                handler.postDelayed(this, 5_000L)
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        val prefs = AppPreferences(requireContext())

        binding.btnSilenceTimed.text = getString(R.string.action_silence_duration, prefs.graceDurationMin)
        binding.btnSilenceTimed.setOnClickListener {
            GracePeriodHelper.startGracePeriod(requireContext(), "quick action")
        }
        binding.btnSilenceIndefinite.setOnClickListener {
            GracePeriodHelper.setIndefinite(requireContext())
            refreshUi()
        }
        binding.btnRingNow.setOnClickListener {
            GracePeriodHelper.ringNow(requireContext())
            refreshUi()
        }
        binding.btnCustomDuration.setOnClickListener { showCustomDurationDialog() }
        binding.setupBannerButton.setOnClickListener {
            PermissionHelper.firstSetupIssue(requireContext())?.let { issue ->
                PermissionHelper.openSetupIssue(requireContext(), issue)
            }
        }
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacks(refreshRunnable)
        _binding = null
        super.onDestroyView()
    }

    private fun refreshUi() {
        val context = requireContext()
        val prefs = AppPreferences(context)
        binding.btnSilenceTimed.text = getString(R.string.action_silence_duration, prefs.graceDurationMin)

        when (prefs.silenceMode) {
            RingerGuardPrefs.SILENCE_TIMED -> {
                binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rg_status_orange_bg))
                binding.statusTitle.setTextColor(ContextCompat.getColor(context, R.color.rg_orange))
                binding.statusTitle.text = getString(
                    R.string.status_silenced,
                    GracePeriodHelper.formatRemaining(prefs.graceUntilMs),
                )
                binding.statusCountdown.visibility = View.GONE
            }
            RingerGuardPrefs.SILENCE_INDEFINITE -> {
                binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rg_status_purple_bg))
                binding.statusTitle.setTextColor(ContextCompat.getColor(context, R.color.rg_purple))
                binding.statusTitle.text = getString(R.string.status_indefinite)
                binding.statusCountdown.visibility = View.GONE
            }
            else -> {
                binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rg_status_green_bg))
                binding.statusTitle.setTextColor(ContextCompat.getColor(context, R.color.rg_green))
                binding.statusTitle.text = getString(R.string.status_ringing)
                binding.statusCountdown.visibility = View.GONE
            }
        }

        val live = RingerStatusHelper.read(context)
        binding.liveVolume.text = "${getString(R.string.live_volume)}: ${live.currentVolume} / ${live.maxVolume}"
        binding.liveDnd.text = "${getString(R.string.live_dnd)}: ${if (live.dndOn) getString(R.string.on) else getString(R.string.off)}"
        binding.liveVibrate.text = "${getString(R.string.live_vibrate)}: ${if (live.vibrateOnly) getString(R.string.on) else getString(R.string.off)}"
        binding.liveService.text = "${getString(R.string.live_service)}: ${if (live.serviceRunning) getString(R.string.yes) else getString(R.string.no)}"

        val issue = PermissionHelper.firstSetupIssue(context)
        if (issue == null) {
            binding.setupBanner.visibility = View.GONE
        } else {
            binding.setupBanner.visibility = View.VISIBLE
            binding.setupBannerText.text = when (issue) {
                SetupIssue.BATTERY -> getString(R.string.setup_banner_battery)
                SetupIssue.EXACT_ALARM -> getString(R.string.setup_banner_exact_alarm)
                SetupIssue.DND -> getString(R.string.setup_banner_dnd)
            }
        }
    }

    private fun showCustomDurationDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(AppPreferences(requireContext()).graceDurationMin.toString())
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_custom_duration)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val minutes = input.text.toString().toIntOrNull()?.coerceIn(1, 24 * 60) ?: return@setPositiveButton
                GracePeriodHelper.startGracePeriod(requireContext(), "custom duration", minutes)
                refreshUi()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
