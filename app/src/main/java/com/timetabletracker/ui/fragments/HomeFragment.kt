package com.timetabletracker.ui.fragments

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.timetabletracker.R
import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.databinding.FragmentHomeBinding
import com.timetabletracker.scheduler.AlarmScheduler
import com.timetabletracker.ui.AppStatus
import com.timetabletracker.ui.MainViewModel
import com.timetabletracker.ui.adapters.TimetableSlotAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val adapter = TimetableSlotAdapter()

    // Map of Date string -> List of DisplaySlot
    private var slotsByDay: Map<String, List<DisplaySlot>> = emptyMap()
    private var selectedDayKey: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTimetableSlots.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTimetableSlots.adapter = adapter

        // Observe Status & Times
        viewModel.lastCheckTime.observe(viewLifecycleOwner) { time ->
            binding.tvLastChecked.text = getString(R.string.last_checked, time)
        }

        viewModel.nextCheckTime.observe(viewLifecycleOwner) { time ->
            binding.tvNextCheck.text = getString(R.string.next_check, time)
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            updateStatusBadge(status)
            binding.fabCheckNow.isEnabled = status != AppStatus.CHECKING
            binding.progressBar.visibility = if (status == AppStatus.CHECKING) View.VISIBLE else View.GONE
        }

        // Observe Timetable Display Slots
        viewModel.displaySlots.observe(viewLifecycleOwner) { displaySlots ->
            renderTimetable(displaySlots ?: emptyList())
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

    /**
     * Groups timetable slots by day/date and renders interactive Day Chips + Slots list.
     */
    private fun renderTimetable(slots: List<DisplaySlot>) {
        if (slots.isEmpty()) {
            binding.layoutEmptyTimetable.visibility = View.VISIBLE
            binding.rvTimetableSlots.visibility = View.GONE
            binding.scrollDayChips.visibility = View.GONE
            binding.layoutLegend.visibility = View.GONE
            binding.tvClassCount.text = "0 classes"
            adapter.submitList(emptyList())
            return
        }

        binding.layoutEmptyTimetable.visibility = View.GONE
        binding.rvTimetableSlots.visibility = View.VISIBLE
        binding.scrollDayChips.visibility = View.VISIBLE
        binding.layoutLegend.visibility = View.VISIBLE
        binding.tvClassCount.text = "${slots.size} classes this week"

        // Group slots by Date (e.g. "20-Oct-2026")
        // Maintain chronological order
        slotsByDay = slots.groupBy { it.slot.slotDate }

        // Setup Day selector chips
        val chipGroup = binding.chipGroupDays
        chipGroup.removeAllViews()

        val sortedDays = slotsByDay.keys.toList()

        sortedDays.forEachIndexed { index, dayKey ->
            val daySlots = slotsByDay[dayKey] ?: emptyList()
            val dayName = daySlots.firstOrNull()?.slot?.dayOfWeekDetails?.dayOfWeekName
                ?: dayKey

            // Short label, e.g. "Mon 20-Oct" or "Tuesday"
            val chip = Chip(requireContext()).apply {
                text = "$dayName (${daySlots.size})"
                isCheckable = true
                isClickable = true
                id = View.generateViewId()

                // Highlight chip if day contains any changed classes
                val hasChangesOnDay = daySlots.any { it.changeStatus != com.timetabletracker.data.api.models.SlotChangeStatus.UNCHANGED }
                if (hasChangesOnDay) {
                    setChipStrokeColorResource(R.color.change_rescheduled_border)
                    chipStrokeWidth = 3f
                }

                setOnClickListener {
                    selectedDayKey = dayKey
                    updateSlotsForSelectedDay(dayKey)
                }
            }

            chipGroup.addView(chip)

            // Auto-select first day or keep previous selection
            if (selectedDayKey == null && index == 0) {
                selectedDayKey = dayKey
                chip.isChecked = true
            } else if (selectedDayKey == dayKey) {
                chip.isChecked = true
            }
        }

        // Show slots for selected day
        val activeDay = selectedDayKey ?: sortedDays.firstOrNull()
        if (activeDay != null) {
            updateSlotsForSelectedDay(activeDay)
        }
    }

    private fun updateSlotsForSelectedDay(dayKey: String) {
        val slots = slotsByDay[dayKey] ?: emptyList()
        // Sort slots chronologically by start time
        val sorted = slots.sortedBy { it.slot.slotStartTime }
        adapter.submitList(sorted)
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
