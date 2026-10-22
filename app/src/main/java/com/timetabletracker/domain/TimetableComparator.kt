package com.timetabletracker.domain

import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.data.api.models.SlotChangeStatus
import com.timetabletracker.data.api.models.TimetableSlot

/**
 * Result of comparing the previous timetable snapshot against the current one.
 * Contains:
 * - [displaySlots]: combined list to show in the UI, containing:
 *     - Current slots (marked UNCHANGED, ADDED, or RESCHEDULED)
 *     - Deleted slots from previous check (marked DELETED with red border)
 * - [hasChanges]: true if any additions, removals, or reschedules occurred
 * - [summary]: human-readable description for system notification
 */
data class ComparisonResult(
    val displaySlots: List<DisplaySlot>,
    val hasChanges: Boolean,
    val summary: String
)

/**
 * Compares [previousSlots] against [currentSlots].
 * Highlights:
 * - Green border for ADDED classes
 * - Red border for DELETED classes (retained from old timetable for visual clarity)
 * - Muted-orange border for RESCHEDULED or MODIFIED classes
 */
object TimetableComparator {

    fun compare(
        previousSlots: List<TimetableSlot>,
        currentSlots: List<TimetableSlot>
    ): ComparisonResult {
        // If this is the very first check, all current slots are considered current/unchanged
        if (previousSlots.isEmpty()) {
            val displayList = currentSlots.map { DisplaySlot(slot = it, changeStatus = SlotChangeStatus.UNCHANGED) }
            return ComparisonResult(
                displaySlots = displayList,
                hasChanges = false,
                summary = "Initial timetable baseline established."
            )
        }

        val previousMap = previousSlots.associateBy { it.toKey() }
        val currentMap = currentSlots.associateBy { it.toKey() }

        val displayList = mutableListOf<DisplaySlot>()
        val changeDescriptions = mutableListOf<String>()

        var addedCount = 0
        var deletedCount = 0
        var modifiedCount = 0

        // 1. Process all current slots
        currentSlots.forEach { curr ->
            val key = curr.toKey()
            val prev = previousMap[key]

            if (prev == null) {
                // Class was ADDED
                addedCount++
                val desc = "Added: ${curr.moduleName()} on ${curr.slotDate} at ${curr.slotStartTime}"
                changeDescriptions.add(desc)
                displayList.add(
                    DisplaySlot(
                        slot = curr,
                        changeStatus = SlotChangeStatus.ADDED,
                        changeDetail = "Added to your timetable"
                    )
                )
            } else {
                // Check if rescheduled / modified
                val mods = mutableListOf<String>()
                if (prev.slotEndTime != curr.slotEndTime) {
                    mods.add("Time: ${curr.slotStartTime} - ${curr.slotEndTime}")
                }
                val prevVenue = prev.floorPlanDetails?.venueName ?: ""
                val currVenue = curr.floorPlanDetails?.venueName ?: ""
                if (prevVenue != currVenue) {
                    mods.add("Room ${prevVenue.ifBlank { "?" }} → ${currVenue.ifBlank { "?" }}")
                }
                val prevTeacher = prev.teacherList?.joinToString { it.name } ?: ""
                val currTeacher = curr.teacherList?.joinToString { it.name } ?: ""
                if (prevTeacher != currTeacher) {
                    mods.add("Faculty: $prevTeacher → $currTeacher")
                }
                if (prev.isCancelledSlot != curr.isCancelledSlot) {
                    mods.add(if (curr.isCancelledSlot) "Cancelled" else "Un-cancelled")
                }
                if (prev.isRescheduledSlot != curr.isRescheduledSlot) {
                    mods.add(if (curr.isRescheduledSlot) "Rescheduled" else "Reschedule reversed")
                }

                if (mods.isNotEmpty() || curr.isRescheduledSlot || curr.isCancelledSlot) {
                    modifiedCount++
                    val detail = mods.joinToString(", ")
                    changeDescriptions.add("Changed: ${curr.moduleName()} ($detail)")
                    displayList.add(
                        DisplaySlot(
                            slot = curr,
                            changeStatus = SlotChangeStatus.RESCHEDULED,
                            changeDetail = detail.ifBlank { "Rescheduled" }
                        )
                    )
                } else {
                    // Unchanged
                    displayList.add(
                        DisplaySlot(
                            slot = curr,
                            changeStatus = SlotChangeStatus.UNCHANGED
                        )
                    )
                }
            }
        }

        // 2. Add DELETED slots from previous check (so they can be highlighted in red)
        previousSlots.forEach { prev ->
            val key = prev.toKey()
            if (!currentMap.containsKey(key)) {
                deletedCount++
                val desc = "Removed: ${prev.moduleName()} on ${prev.slotDate} at ${prev.slotStartTime}"
                changeDescriptions.add(desc)
                displayList.add(
                    DisplaySlot(
                        slot = prev,
                        changeStatus = SlotChangeStatus.DELETED,
                        changeDetail = "Removed from your timetable"
                    )
                )
            }
        }

        val hasChanges = addedCount > 0 || deletedCount > 0 || modifiedCount > 0

        val summary = if (hasChanges) {
            buildString {
                val parts = mutableListOf<String>()
                if (addedCount > 0) parts.add("$addedCount added")
                if (deletedCount > 0) parts.add("$deletedCount removed")
                if (modifiedCount > 0) parts.add("$modifiedCount changed")
                append("${parts.joinToString(", ")}.\n")
                append(changeDescriptions.take(5).joinToString("\n"))
            }
        } else {
            "No changes detected."
        }

        return ComparisonResult(
            displaySlots = displayList,
            hasChanges = hasChanges,
            summary = summary
        )
    }

    private fun TimetableSlot.toKey(): String {
        val moduleCode = moduleList?.firstOrNull()?.moduleCode ?: "UNKNOWN"
        return "${slotDate}_${slotStartTime}_${moduleCode}"
    }

    private fun TimetableSlot.moduleName(): String =
        moduleList?.firstOrNull()?.let { "${it.moduleName} (${it.moduleCode})" } ?: "Unknown Module"
}
