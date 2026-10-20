package com.timetabletracker.ui.adapters

import android.content.Context
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.timetabletracker.R
import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.data.api.models.SlotChangeStatus
import com.timetabletracker.databinding.ItemTimetableSlotBinding

/**
 * Adapter for rendering timetable slots.
 * Highlights:
 * - Green border for ADDED classes
 * - Red border for DELETED classes (with strike-through styling)
 * - Muted-orange border for RESCHEDULED classes
 * - Interactive: Clicking any slot reveals full details in a bottom/dialog alert.
 */
class TimetableSlotAdapter :
    ListAdapter<DisplaySlot, TimetableSlotAdapter.SlotViewHolder>(SlotDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemTimetableSlotBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SlotViewHolder(private val binding: ItemTimetableSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(displaySlot: DisplaySlot) {
            val context = binding.root.context
            val slot = displaySlot.slot

            // Time range
            val timeText = if (slot.slotStartTime.isNotBlank()) {
                "${slot.slotStartTime} - ${slot.slotEndTime}"
            } else {
                "Time TBD"
            }
            binding.tvSlotTime.text = timeText

            // Subject / Module Name
            val module = slot.moduleList?.firstOrNull()
            val subjectTitle = if (module != null && module.moduleName.isNotBlank()) {
                "${module.moduleName} (${module.moduleCode})"
            } else {
                "Class Slot"
            }
            binding.tvModuleName.text = subjectTitle

            // Venue
            val venue = slot.floorPlanDetails?.venueName
            val venueCat = slot.floorPlanDetails?.venueCategory
            binding.tvVenue.text = when {
                !venue.isNullOrBlank() && !venueCat.isNullOrBlank() -> "📍 Room $venue ($venueCat)"
                !venue.isNullOrBlank() -> "📍 Room $venue"
                else -> "📍 Venue TBD"
            }

            // Teacher
            val teacherName = slot.teacherList?.firstOrNull()?.name
            binding.tvTeacher.text = if (!teacherName.isNullOrBlank()) {
                "👤 $teacherName"
            } else {
                "👤 Faculty TBD"
            }

            // Highlight border and styling based on change status
            when (displaySlot.changeStatus) {
                SlotChangeStatus.ADDED -> {
                    binding.cardSlot.setBackgroundResource(R.drawable.bg_slot_added)
                    binding.tvChangeBadge.visibility = View.VISIBLE
                    binding.tvChangeBadge.text = "ADDED"
                    binding.tvChangeBadge.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.change_added_badge)
                    )
                    binding.tvModuleName.paintFlags = binding.tvModuleName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                SlotChangeStatus.DELETED -> {
                    binding.cardSlot.setBackgroundResource(R.drawable.bg_slot_deleted)
                    binding.tvChangeBadge.visibility = View.VISIBLE
                    binding.tvChangeBadge.text = "DELETED"
                    binding.tvChangeBadge.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.change_deleted_badge)
                    )
                    // Strike through deleted class
                    binding.tvModuleName.paintFlags = binding.tvModuleName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }
                SlotChangeStatus.RESCHEDULED -> {
                    binding.cardSlot.setBackgroundResource(R.drawable.bg_slot_rescheduled)
                    binding.tvChangeBadge.visibility = View.VISIBLE
                    binding.tvChangeBadge.text = "RESCHEDULED"
                    binding.tvChangeBadge.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.change_rescheduled_badge)
                    )
                    binding.tvModuleName.paintFlags = binding.tvModuleName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                SlotChangeStatus.UNCHANGED -> {
                    binding.cardSlot.setBackgroundResource(R.drawable.bg_slot_unchanged)
                    binding.tvChangeBadge.visibility = View.GONE
                    binding.tvModuleName.paintFlags = binding.tvModuleName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
            }

            // Change description note if present
            if (!displaySlot.changeDetail.isNullOrBlank() && displaySlot.changeStatus != SlotChangeStatus.UNCHANGED) {
                binding.tvChangeDetail.visibility = View.VISIBLE
                binding.tvChangeDetail.text = "ℹ️ ${displaySlot.changeDetail}"
            } else {
                binding.tvChangeDetail.visibility = View.GONE
            }

            // Interactive click: Show detailed class dialog
            binding.root.setOnClickListener {
                showSlotDetailsDialog(context, displaySlot)
            }
        }

        private fun showSlotDetailsDialog(context: Context, displaySlot: DisplaySlot) {
            val slot = displaySlot.slot
            val module = slot.moduleList?.firstOrNull()
            val day = slot.dayOfWeekDetails?.dayOfWeekName ?: ""
            val faculty = slot.teacherList?.joinToString { it.name } ?: "Not specified"
            val venue = slot.floorPlanDetails?.venueName ?: "Not specified"
            val meetingLink = slot.floorPlanDetails?.meetingLink

            val detailsText = buildString {
                append("📅 Date: ${slot.slotDate} ($day)\n")
                append("⏰ Time: ${slot.slotStartTime} - ${slot.slotEndTime}\n")
                append("📚 Subject: ${module?.moduleName ?: "N/A"}\n")
                append("🔢 Code: ${module?.moduleCode ?: "N/A"}\n")
                append("📍 Room: $venue\n")
                append("👤 Faculty: $faculty\n")
                if (!meetingLink.isNullOrBlank()) {
                    append("\n🔗 Teams Link: $meetingLink\n")
                }
                if (!displaySlot.changeDetail.isNullOrBlank()) {
                    append("\n🔔 Change Note:\n${displaySlot.changeDetail}")
                }
            }

            AlertDialog.Builder(context)
                .setTitle(module?.moduleName ?: "Class Details")
                .setMessage(detailsText)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    companion object SlotDiffCallback : DiffUtil.ItemCallback<DisplaySlot>() {
        override fun areItemsTheSame(oldItem: DisplaySlot, newItem: DisplaySlot): Boolean {
            val oldSlot = oldItem.slot
            val newSlot = newItem.slot
            val oldModule = oldSlot.moduleList?.firstOrNull()?.moduleCode
            val newModule = newSlot.moduleList?.firstOrNull()?.moduleCode
            return oldSlot.slotDate == newSlot.slotDate &&
                    oldSlot.slotStartTime == newSlot.slotStartTime &&
                    oldModule == newModule
        }

        override fun areContentsTheSame(oldItem: DisplaySlot, newItem: DisplaySlot): Boolean =
            oldItem == newItem
    }
}
