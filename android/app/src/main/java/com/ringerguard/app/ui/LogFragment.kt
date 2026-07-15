package com.ringerguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ringerguard.app.EventLogger
import com.ringerguard.app.R
import com.ringerguard.app.databinding.FragmentLogBinding
import com.ringerguard.app.databinding.ItemLogEntryBinding

class LogFragment : Fragment(R.layout.fragment_log) {
    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLogBinding.bind(view)
        binding.logRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.btnClearLog.setOnClickListener { confirmClear() }
        refreshLog()
    }

    override fun onResume() {
        super.onResume()
        refreshLog()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun refreshLog() {
        val context = requireContext()
        val entries = EventLogger.getAll(context)
        binding.logRecycler.adapter = LogAdapter(entries)
        binding.summaryText.text = EventLogger.sevenDaySummary(context)
    }

    private fun confirmClear() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.clear_log_confirm)
            .setPositiveButton(R.string.ok) { _, _ ->
                EventLogger.clear(requireContext())
                refreshLog()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private class LogAdapter(
        private val entries: List<com.ringerguard.app.LogEntry>,
    ) : RecyclerView.Adapter<LogAdapter.Holder>() {
        class Holder(val binding: ItemLogEntryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemLogEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val entry = entries[position]
            holder.binding.logItemText.text =
                "${EventLogger.formatTime(entry.timestampMs)} — ${entry.title}\n${entry.detail}"
        }

        override fun getItemCount(): Int = entries.size
    }
}
