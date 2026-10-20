package com.timetabletracker.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.timetabletracker.data.api.models.TimetableSlot
import java.io.File
import java.io.Serializable

/**
 * Represents a past timetable change check event, stored in history.json.
 */
data class ChangeEvent(
    val id: Long = System.currentTimeMillis(),
    val checkTime: String = "",
    val summary: String = "",
    val hasChanges: Boolean = false,
    val changeDetails: String = ""
) : Serializable

/**
 * Handles persistent storage of timetable snapshots and change history.
 * Snapshots are stored as individual JSON files in filesDir/snapshots/.
 * History is stored as filesDir/history.json (max 50 entries).
 */
class SnapshotStorage(private val context: Context) {

    companion object {
        private const val SNAPSHOT_DIR = "snapshots"
        private const val HISTORY_FILE = "history.json"
        private const val CURRENT_SNAPSHOT_FILE = "current_snapshot.json"
        private const val MAX_HISTORY_ENTRIES = 50
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val snapshotDir: File
        get() = File(context.filesDir, SNAPSHOT_DIR).also { it.mkdirs() }

    private val historyFile: File
        get() = File(context.filesDir, HISTORY_FILE)

    // ─── Snapshots ───────────────────────────────────────────────────────────

    /**
     * Saves the current week's timetable slots as the latest snapshot.
     */
    fun saveSnapshot(slots: List<TimetableSlot>) {
        val file = File(snapshotDir, CURRENT_SNAPSHOT_FILE)
        file.writeText(gson.toJson(slots))
    }

    /**
     * Loads the previous snapshot, or null if none exists.
     */
    fun loadPreviousSnapshot(): List<TimetableSlot>? {
        val file = File(snapshotDir, CURRENT_SNAPSHOT_FILE)
        if (!file.exists()) return null
        return try {
            val type = object : TypeToken<List<TimetableSlot>>() {}.type
            gson.fromJson(file.readText(), type)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes all stored snapshots.
     */
    fun clearSnapshots() {
        snapshotDir.listFiles()?.forEach { it.delete() }
    }

    // ─── History ─────────────────────────────────────────────────────────────

    /**
     * Appends a new change event to the history log.
     * Trims the list to MAX_HISTORY_ENTRIES.
     */
    fun appendHistory(event: ChangeEvent) {
        val history = loadHistory().toMutableList()
        history.add(0, event) // newest first
        if (history.size > MAX_HISTORY_ENTRIES) {
            history.subList(MAX_HISTORY_ENTRIES, history.size).clear()
        }
        historyFile.writeText(gson.toJson(history))
    }

    /**
     * Loads all stored change events.
     */
    fun loadHistory(): List<ChangeEvent> {
        if (!historyFile.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<ChangeEvent>>() {}.type
            gson.fromJson(historyFile.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Deletes the history log.
     */
    fun clearHistory() {
        historyFile.delete()
    }
}
