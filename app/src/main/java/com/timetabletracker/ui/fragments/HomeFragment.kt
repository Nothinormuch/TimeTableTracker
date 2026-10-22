package com.timetabletracker.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.timetabletracker.R
import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.data.api.models.SlotChangeStatus
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

        viewModel.lastCheckTime.observe(viewLifecycleOwner) { time ->
            binding.tvLastChecked.text = getString(R.string.last_checked, time)
        }

        viewModel.nextCheckTime.observe(viewLifecycleOwner) { time ->
            binding.tvNextCheck.text = getString(R.string.next_check, time)
        }

        viewModel.status.observe(viewLifecycleOwner) { status ->
            updateStatusBadge(status)
            binding.fabCheckNow.isEnabled = status != AppStatus.CHECKING
            binding.progressBar.visibility =
                if (status == AppStatus.CHECKING) View.VISIBLE else View.GONE
        }

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
            binding.tvClassCount.text = getString(R.string.classes_count, 0)
            adapter.submitList(emptyList())
            return
        }

        binding.layoutEmptyTimetable.visibility = View.GONE
        binding.rvTimetableSlots.visibility = View.VISIBLE
        binding.scrollDayChips.visibility = View.VISIBLE
        binding.tvClassCount.text = getString(R.string.classes_count, slots.size)

        // Group slots by Date (e.g. "20-Oct-2026"), maintaining chronological order
        slotsByDay = slots.groupBy { it.slot.slotDate }

        val chipGroup = binding.chipGroupDays
        chipGroup.removeAllViews()

        val sortedDays = slotsByDay.keys.toList()

        sortedDays.forEachIndexed { index, dayKey ->
            val daySlots = slotsByDay[dayKey] ?: emptyList()
            val dayName = daySlots.firstOrNull()?.slot?.dayOfWeekDetails?.dayOfWeekName
                ?: dayKey

            val chip = Chip(requireContext()).apply {
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                chipBackgroundColor = ContextCompat.getColorStateList(
                    requireContext(), R.color.chip_day_bg
                )
                setTextColor(
                    ContextCompat.getColorStateList(requireContext(), R.color.chip_day_text)
                )
                chipStrokeWidth = 0f
                chipCornerRadius = 20f * resources.displayMetrics.density
                chipMinHeight = 36f * resources.displayMetrics.density
                isChipIconVisible = false
                text = getString(R.string.day_chip_label, dayName, daySlots.size)
                isCheckable = true
                isClickable = true
                id = View.generateViewId()

                // Mark days that contain changes with a small colored dot in the label
                val changedCount = daySlots.count {
                    it.changeStatus != SlotChangeStatus.UNCHANGED
                }
                if (changedCount > 0) {
                    text = getString(R.string.day_chip_label, dayName, daySlots.size) + " •"
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

        val activeDay = selectedDayKey ?: sortedDays.firstOrNull()
        if (activeDay != null) {
            updateSlotsForSelectedDay(activeDay)
        }
    }

    private fun updateSlotsForSelectedDay(dayKey: String) {
        val slots = slotsByDay[dayKey] ?: emptyList()
        val sorted = slots.sortedWith(compareBy { parse24Hour(it.slot.slotStartTime) })
        adapter.submitList(sorted)
    }

    private fun parse24Hour(timeStr: String): Int {
        if (timeStr.isBlank()) return 0
        val regex = """(\d{1,2}):(\d{2})\s*(AM|PM)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(timeStr.trim()) ?: return 0
        val (hourStr, minuteStr, amPm) = match.destructured
        var hour = hourStr.toInt()
        val minute = minuteStr.toInt()
        val isPm = amPm.equals("PM", ignoreCase = true)
        val isAm = amPm.equals("AM", ignoreCase = true)
        if (isPm && hour != 12) hour += 12
        if (isAm && hour == 12) hour = 0
        return hour * 60 + minute
    }

    private fun updateStatusBadge(status: AppStatus) {
        val (colorRes, textRes) = when (status) {
            AppStatus.IDLE -> Pair(R.color.status_idle, R.string.status_idle)
            AppStatus.CHECKING -> Pair(R.color.status_idle, R.string.status_checking)
            AppStatus.CHANGES_FOUND -> Pair(R.color.status_changes, R.string.status_changes)
            AppStatus.NO_CHANGES -> Pair(R.color.status_ok, R.string.status_no_changes)
            AppStatus.ERROR -> Pair(R.color.status_error, R.string.status_error)
        }
        binding.tvStatusBadge.text = getString(textRes)
        binding.tvStatusBadge.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), colorRes)
        binding.tvStatusBadge.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.on_status_idle)
        )
    }

    private fun showExactAlarmPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alarm_dialog_title)
            .setMessage(R.string.alarm_dialog_body)
            .setPositiveButton(R.string.alarm_dialog_open) { _, _ ->
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
            .setNegativeButton(R.string.alarm_dialog_dismiss, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
