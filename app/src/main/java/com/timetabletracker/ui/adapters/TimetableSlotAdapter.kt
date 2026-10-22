package com.timetabletracker.ui.adapters

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.timetabletracker.R
import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.data.api.models.SlotChangeStatus
import com.timetabletracker.databinding.ItemTimetableSlotBinding

/**
 * Adapter for rendering timetable slots.
 * Change state is communicated without emoji:
 * - Left accent bar + solid container tint per status (added / removed / changed)
 * - Rounded badge with status label
 * - Removed classes get strikethrough on the subject
 * Clicking a slot opens a Material detail dialog.
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

            // Time range (normalized to 24h for a compact, scannable look)
            binding.tvSlotTime.text = if (slot.slotStartTime.isNotBlank()) {
                "${fmtTime(slot.slotStartTime)} – ${fmtTime(slot.slotEndTime)}"
            } else {
                context.getString(R.string.detail_unspecified)
            }

            // Subject / module
            val module = slot.moduleList?.firstOrNull()
            binding.tvModuleName.text = if (module != null && module.moduleName.isNotBlank()) {
                module.moduleName
            } else {
                context.getString(R.string.detail_unspecified)
            }

            // Venue
            val venue = slot.floorPlanDetails?.venueName
            val venueCat = slot.floorPlanDetails?.venueCategory
            binding.tvVenue.text = when {
                !venue.isNullOrBlank() && !venueCat.isNullOrBlank() -> "Room $venue · $venueCat"
                !venue.isNullOrBlank() -> "Room $venue"
                else -> context.getString(R.string.detail_unspecified)
            }

            // Teacher
            val teacherName = slot.teacherList?.firstOrNull()?.name
            binding.tvTeacher.text = if (!teacherName.isNullOrBlank()) teacherName
            else context.getString(R.string.detail_unspecified)

            // Change state: accent bar color, container tint, badge
            val (barColorRes, containerColorRes, badgeRes, badgeTextRes) = when (displaySlot.changeStatus) {
                SlotChangeStatus.ADDED -> Quad(
                    R.color.change_added_border,
                    R.color.change_added_container,
                    R.color.change_added_border,
                    R.string.badge_added
                )
                SlotChangeStatus.DELETED -> Quad(
                    R.color.change_deleted_border,
                    R.color.change_deleted_container,
                    R.color.change_deleted_border,
                    R.string.badge_deleted
                )
                SlotChangeStatus.RESCHEDULED -> Quad(
                    R.color.change_rescheduled_border,
                    R.color.change_rescheduled_container,
                    R.color.change_rescheduled_border,
                    R.string.badge_rescheduled
                )
                SlotChangeStatus.UNCHANGED -> Quad(
                    R.color.outline_variant,
                    R.color.surface,
                    0,
                    0
                )
            }

            binding.viewChangeBar.setBackgroundColor(
                ContextCompat.getColor(context, barColorRes)
            )
            binding.cardSlot.setCardBackgroundColor(
                ContextCompat.getColor(context, containerColorRes)
            )

            if (badgeRes != 0) {
                binding.tvChangeBadge.visibility = View.VISIBLE
                binding.tvChangeBadge.text = context.getString(badgeTextRes)
                binding.tvChangeBadge.backgroundTintList =
                    ContextCompat.getColorStateList(context, badgeRes)
                binding.tvChangeBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.on_status_idle)
                )
            } else {
                binding.tvChangeBadge.visibility = View.GONE
            }

            // Strikethrough removed subjects
            if (displaySlot.changeStatus == SlotChangeStatus.DELETED) {
                binding.tvModuleName.paintFlags =
                    binding.tvModuleName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvModuleName.paintFlags =
                    binding.tvModuleName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Change detail row (old → new format produced by the comparator)
            if (!displaySlot.changeDetail.isNullOrBlank() &&
                displaySlot.changeStatus != SlotChangeStatus.UNCHANGED
            ) {
                binding.layoutChangeDetail.visibility = View.VISIBLE
                binding.tvChangeDetail.text = displaySlot.changeDetail
            } else {
                binding.layoutChangeDetail.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                showSlotDetailsDialog(context, displaySlot)
            }
        }

        private fun showSlotDetailsDialog(context: android.content.Context, displaySlot: DisplaySlot) {
            val slot = displaySlot.slot
            val module = slot.moduleList?.firstOrNull()
            val faculty = slot.teacherList?.joinToString { it.name }?.ifBlank { null }
            val venue = slot.floorPlanDetails?.venueName?.takeIf { it.isNotBlank() }
            val meetingLink = slot.floorPlanDetails?.meetingLink?.takeIf { it.isNotBlank() }

            val unspecified = context.getString(R.string.detail_unspecified)
            val message = buildString {
                append("${context.getString(R.string.detail_date)}: ${slot.slotDate} " +
                        "(${slot.dayOfWeekDetails?.dayOfWeekName.orEmpty()})\n")
                append("${context.getString(R.string.detail_time)}: " +
                        "${fmtTime(slot.slotStartTime)} – ${fmtTime(slot.slotEndTime)}\n")
                append("${context.getString(R.string.detail_code)}: " +
                        "${module?.moduleCode?.ifBlank { null } ?: unspecified}\n")
                append("${context.getString(R.string.detail_room)}: ${venue ?: unspecified}\n")
                append("${context.getString(R.string.detail_faculty)}: ${faculty ?: unspecified}")
                if (!displaySlot.changeDetail.isNullOrBlank() &&
                    displaySlot.changeStatus != SlotChangeStatus.UNCHANGED
                ) {
                    append("\n\n${displaySlot.changeDetail}")
                }
            }

            MaterialAlertDialogBuilder(context)
                .setTitle(module?.moduleName?.ifBlank { null } ?: context.getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton(R.string.detail_close, null)
                .apply {
                    if (meetingLink != null) {
                        setNeutralButton(R.string.detail_link) { _, _ ->
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(meetingLink))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
                .show()
        }

        /** Converts "09:00 AM" → "09:00" for a compact, sortable display. */
        private fun fmtTime(timeStr: String): String {
            if (timeStr.isBlank()) return ""
            val regex = """(\d{1,2}):(\d{2})\s*(AM|PM)""".toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(timeStr.trim()) ?: return timeStr
            val (hourStr, minuteStr, amPm) = match.destructured
            var hour = hourStr.toInt()
            if (amPm.equals("PM", ignoreCase = true) && hour != 12) hour += 12
            if (amPm.equals("AM", ignoreCase = true) && hour == 12) hour = 0
            return "%02d:%s".format(hour, minuteStr)
        }
    }

    /** Minimal tuple for the per-status styling quadruple. */
    private data class Quad(
        val barColor: Int,
        val containerColor: Int,
        val badgeColor: Int,
        val badgeText: Int
    )

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
