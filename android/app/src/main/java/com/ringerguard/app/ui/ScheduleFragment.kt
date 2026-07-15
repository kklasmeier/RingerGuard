package com.ringerguard.app.ui

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.ringerguard.app.AppPreferences
import com.ringerguard.app.QuietHoursHelper
import com.ringerguard.app.R
import com.ringerguard.app.databinding.FragmentScheduleBinding
import java.util.Calendar

class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val dayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScheduleBinding.bind(view)
        val prefs = AppPreferences(requireContext())

        binding.switchQuietHours.isChecked = prefs.quietHoursEnabled
        binding.switchQuietHours.setOnCheckedChangeListener { _, checked ->
            prefs.quietHoursEnabled = checked
            refreshStatus()
        }

        binding.btnQuietStart.text = QuietHoursHelper.formatTime(prefs.quietStartHour, prefs.quietStartMin)
        binding.btnQuietEnd.text = QuietHoursHelper.formatTime(prefs.quietEndHour, prefs.quietEndMin)

        binding.btnQuietStart.setOnClickListener {
            showTimePicker(prefs.quietStartHour, prefs.quietStartMin) { hour, minute ->
                prefs.quietStartHour = hour
                prefs.quietStartMin = minute
                binding.btnQuietStart.text = QuietHoursHelper.formatTime(hour, minute)
            }
        }
        binding.btnQuietEnd.setOnClickListener {
            showTimePicker(prefs.quietEndHour, prefs.quietEndMin) { hour, minute ->
                prefs.quietEndHour = hour
                prefs.quietEndMin = minute
                binding.btnQuietEnd.text = QuietHoursHelper.formatTime(hour, minute)
            }
        }

        setupDayChips()
        refreshStatus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupDayChips() {
        val prefs = AppPreferences(requireContext())
        val activeDays = prefs.quietDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        binding.dayChipGroup.removeAllViews()
        val chips = mutableListOf<Chip>()
        dayLabels.forEachIndexed { index, label ->
            val dayValue = index + 1
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = dayValue in activeDays
            }
            chips.add(chip)
            binding.dayChipGroup.addView(chip)
        }
        chips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                val selected = chips.mapIndexedNotNull { index, c ->
                    if (c.isChecked) index + 1 else null
                }
                prefs.quietDays = selected.joinToString(",")
                refreshStatus()
            }
        }
    }

    private fun refreshStatus() {
        val inQuiet = QuietHoursHelper.isInQuietHours(requireContext())
        binding.scheduleStatus.text = if (inQuiet) {
            getString(R.string.status_quiet_hours)
        } else {
            getString(R.string.status_active_hours)
        }
    }

    private fun showTimePicker(hour: Int, minute: Int, onSet: (Int, Int) -> Unit) {
        TimePickerDialog(requireContext(), { _, h, m -> onSet(h, m) }, hour, minute, false).show()
    }
}
