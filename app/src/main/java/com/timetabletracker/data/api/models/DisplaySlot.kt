package com.timetabletracker.data.api.models

import java.io.Serializable

enum class SlotChangeStatus {
    UNCHANGED,
    ADDED,         // Green border
    DELETED,       // Red border (from previous timetable)
    RESCHEDULED    // Muted-orange border
}

/**
 * Wraps a TimetableSlot with its change status relative to the previous check.
 */
data class DisplaySlot(
    val slot: TimetableSlot,
    val changeStatus: SlotChangeStatus = SlotChangeStatus.UNCHANGED,
    val changeDetail: String? = null
) : Serializable
