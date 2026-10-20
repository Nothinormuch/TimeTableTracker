package com.timetabletracker.ui.fragments

import android.app.AlarmManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.snackbar.Snackbar
import com.timetabletracker.R
import com.timetabletracker.databinding.FragmentHomeBinding
import com.timetabletracker.scheduler.AlarmScheduler
import com.timetabletracker.ui.AppStatus
import com.timetabletracker.ui.MainViewModel
import com.timetabletracker.ui.adapters.ChangeHistoryAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val adapter = ChangeHistoryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvRecentChanges.adapter = adapter

        // Observe LiveData
        viewModel.lastCheckTime.observe(viewLifecycleOwner) { time ->
            binding.tvLastChecked.text = getString(R.string.last_checked, time)
        }

        viewModel.nextCheckTime.observe(viewLifecycleOwner) { time ->
            binding.tvNextCheck.text = getString(R.string.next_check, time)
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            updateStatusBadge(status)
            binding.fabCheckNow.isEnabled = status != AppStatus.CHECKING
            if (status == AppStatus.CHECKING) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }

        viewModel.changeHistory.observe(viewLifecycleOwner) { history ->
            val recent = history.take(5)
            adapter.submitList(recent)
            binding.tvNoChanges.visibility = if (recent.isEmpty()) View.VISIBLE else View.GONE
            binding.rvRecentChanges.visibility = if (recent.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        // FAB — Check Now
        binding.fabCheckNow.setOnClickListener {
            if (!AlarmScheduler.canScheduleExactAlarms(requireContext())) {
                showExactAlarmPermissionDialog()
                return@setOnClickListener
            }
            viewModel.checkNow()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUiState()
    }

    private fun updateStatusBadge(status: AppStatus) {
        val (colorRes, text) = when (status) {
            AppStatus.IDLE -> Pair(R.color.status_idle, "Idle")
            AppStatus.CHECKING -> Pair(R.color.status_idle, "Checking…")
            AppStatus.CHANGES_FOUND -> Pair(R.color.status_changes, "Changes Found")
            AppStatus.NO_CHANGES -> Pair(R.color.status_ok, "No Changes")
            AppStatus.ERROR -> Pair(R.color.status_error, "Error")
        }
        binding.tvStatusBadge.text = text
        binding.tvStatusBadge.setBackgroundColor(
            ContextCompat.getColor(requireContext(), colorRes)
        )
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("To check the timetable at the exact scheduled time, please grant the 'Alarms & Reminders' permission in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
