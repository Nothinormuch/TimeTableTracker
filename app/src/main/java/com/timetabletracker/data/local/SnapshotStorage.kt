package com.timetabletracker.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.timetabletracker.data.api.models.DisplaySlot
import com.timetabletracker.data.api.models.TimetableSlot
import java.io.File

/**
 * Handles persistent storage of timetable snapshots.
 *
 * Requirements:
 * - At any instance, we only store the timetable from the previous check.
 * - As soon as changes are detected, we note the changes, keep the new timetable,
 *   and remove the old one.
 * - Stores display slots (with change highlights: added, deleted, rescheduled).
 */
class SnapshotStorage(private val context: Context) {

    companion object {
        private const val SNAPSHOT_DIR = "snapshots"
        private const val TIMETABLE_SNAPSHOT_FILE = "timetable_snapshot.json"
        private const val DISPLAY_SLOTS_FILE = "display_slots.json"
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val snapshotDir: File
        get() = File(context.filesDir, SNAPSHOT_DIR).also { it.mkdirs() }

    private val snapshotFile: File
        get() = File(snapshotDir, TIMETABLE_SNAPSHOT_FILE)

    private val displaySlotsFile: File
        get() = File(snapshotDir, DISPLAY_SLOTS_FILE)

    // ─── Raw Timetable Snapshot (previous check's timetable) ──────────────────

    /**
     * Loads the stored timetable from the previous check, or null if none exists.
     */
    fun loadPreviousSnapshot(): List<TimetableSlot>? {
        if (!snapshotFile.exists()) return null
        return try {
            val type = object : TypeToken<List<TimetableSlot>>() {}.type
            gson.fromJson(snapshotFile.readText(), type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Overwrites the stored timetable snapshot with the newly fetched timetable.
     * This ensures the old timetable is discarded and only the current one is kept.
     */
    fun saveSnapshot(slots: List<TimetableSlot>) {
        snapshotFile.writeText(gson.toJson(slots))
    }

    // ─── Display Slots (current view including change highlights) ─────────────

    /**
     * Saves the annotated display slots (including added, deleted, and rescheduled flags).
     */
    fun saveDisplaySlots(displaySlots: List<DisplaySlot>) {
        displaySlotsFile.writeText(gson.toJson(displaySlots))
    }

    /**
     * Loads the annotated display slots for UI presentation.
     */
    fun loadDisplaySlots(): List<DisplaySlot> {
        if (!displaySlotsFile.exists()) {
            // Fallback: if no annotated display slots exist, load raw snapshot as unchanged
            return loadPreviousSnapshot()?.map { DisplaySlot(it) } ?: emptyList()
        }
        return try {
            val type = object : TypeToken<List<DisplaySlot>>() {}.type
            gson.fromJson(displaySlotsFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clears all stored timetable files.
     */
    fun clearSnapshots() {
        snapshotDir.listFiles()?.forEach { it.delete() }
    }
}
