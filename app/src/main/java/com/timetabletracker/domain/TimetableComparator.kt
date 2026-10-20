package com.timetabletracker.domain

import com.timetabletracker.data.api.models.TimetableSlot

/**
 * Describes a single detected change in the timetable.
 */
data class TimetableChange(
    val type: ChangeType,
    val key: String,
    val description: String
)

enum class ChangeType {
    ADDED, REMOVED, MODIFIED
}

/**
 * Compares two lists of TimetableSlots and produces a human-readable change report.
 *
 * Slot key: SlotDate + "_" + SlotStartTime + "_" + ModuleCode
 */
object TimetableComparator {

    /**
     * Compares [previousSlots] against [currentSlots] and returns
     * a list of detected changes plus a formatted summary string.
     */
    fun compare(
        previousSlots: List<TimetableSlot>,
        currentSlots: List<TimetableSlot>
    ): ComparisonResult {
        val previousMap = previousSlots.associateBy { it.toKey() }
        val currentMap = currentSlots.associateBy { it.toKey() }

        val changes = mutableListOf<TimetableChange>()

        // Detect removed slots
        previousMap.forEach { (key, slot) ->
            if (!currentMap.containsKey(key)) {
                changes.add(
                    TimetableChange(
                        type = ChangeType.REMOVED,
                        key = key,
                        description = "❌ REMOVED: ${slot.moduleName()} on ${slot.slotDate} at ${slot.slotStartTime}"
                    )
                )
            }
        }

        // Detect added slots
        currentMap.forEach { (key, slot) ->
            if (!previousMap.containsKey(key)) {
                changes.add(
                    TimetableChange(
                        type = ChangeType.ADDED,
                        key = key,
                        description = "✅ ADDED: ${slot.moduleName()} on ${slot.slotDate} at ${slot.slotStartTime} " +
                                "in ${slot.floorPlanDetails?.venueName ?: "TBD"}"
                    )
                )
            }
        }

        // Detect modifications on matching keys
        previousMap.forEach { (key, prev) ->
            val curr = currentMap[key] ?: return@forEach
            val modificationDetails = mutableListOf<String>()

            if (prev.slotEndTime != curr.slotEndTime) {
                modificationDetails.add("End time: ${prev.slotEndTime} → ${curr.slotEndTime}")
            }
            val prevVenue = prev.floorPlanDetails?.venueName ?: ""
            val currVenue = curr.floorPlanDetails?.venueName ?: ""
            if (prevVenue != currVenue) {
                modificationDetails.add("Venue: $prevVenue → $currVenue")
            }
            val prevTeacher = prev.teacherList?.joinToString { it.name } ?: ""
            val currTeacher = curr.teacherList?.joinToString { it.name } ?: ""
            if (prevTeacher != currTeacher) {
                modificationDetails.add("Instructor: $prevTeacher → $currTeacher")
            }
            if (prev.isCancelledSlot != curr.isCancelledSlot) {
                val status = if (curr.isCancelledSlot) "now CANCELLED" else "cancellation REVERSED"
                modificationDetails.add("Status: $status")
            }
            if (prev.isRescheduledSlot != curr.isRescheduledSlot) {
                val status = if (curr.isRescheduledSlot) "now RESCHEDULED" else "reschedule REVERSED"
                modificationDetails.add("Status: $status")
            }

            if (modificationDetails.isNotEmpty()) {
                changes.add(
                    TimetableChange(
                        type = ChangeType.MODIFIED,
                        key = key,
                        description = "✏️ MODIFIED: ${prev.moduleName()} on ${prev.slotDate} at ${prev.slotStartTime}\n" +
                                modificationDetails.joinToString("\n  • ", prefix = "  • ")
                    )
                )
            }
        }

        val summary = buildSummary(changes)
        return ComparisonResult(changes = changes, summary = summary)
    }

    private fun TimetableSlot.toKey(): String {
        val moduleCode = moduleList?.firstOrNull()?.moduleCode ?: "UNKNOWN"
        return "${slotDate}_${slotStartTime}_${moduleCode}"
    }

    private fun TimetableSlot.moduleName(): String =
        moduleList?.firstOrNull()?.let { "${it.moduleName} (${it.moduleCode})" } ?: "Unknown Module"

    private fun buildSummary(changes: List<TimetableChange>): String {
        if (changes.isEmpty()) return "No changes detected."
        val added = changes.count { it.type == ChangeType.ADDED }
        val removed = changes.count { it.type == ChangeType.REMOVED }
        val modified = changes.count { it.type == ChangeType.MODIFIED }

        val header = buildString {
            append("🔔 Timetable Changes Detected!\n")
            if (added > 0) append("• $added class(es) added\n")
            if (removed > 0) append("• $removed class(es) removed\n")
            if (modified > 0) append("• $modified class(es) modified\n")
            append("\nDetails:\n")
        }
        val details = changes.joinToString("\n\n") { it.description }
        return header + details
    }
}

data class ComparisonResult(
    val changes: List<TimetableChange>,
    val summary: String
) {
    val hasChanges: Boolean get() = changes.isNotEmpty()
}
