package com.timetabletracker.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.timetabletracker.data.local.PreferencesManager
import com.timetabletracker.data.local.SnapshotStorage
import com.timetabletracker.data.repository.TimetableRepository
import com.timetabletracker.domain.TimetableComparator
import com.timetabletracker.notification.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager CoroutineWorker that performs the actual timetable check.
 * Enqueued by AlarmReceiver on each scheduled alarm or triggered manually.
 *
 * Flow:
 * 1. Load previous snapshot (the timetable from the previous check).
 * 2. Fetch current timetable from UPES API.
 * 3. Compare current vs previous to determine added, deleted, and rescheduled slots.
 * 4. Save annotated display slots (with change highlight flags).
 * 5. Save current timetable snapshot (replacing previous timetable).
 * 6. Send system notification if changes detected.
 */
class TimetableFetchWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TimetableFetchWorker"
        const val WORK_NAME = "TimetableFetchWork"
        const val WORK_NAME_IMMEDIATE = "TimetableFetchWorkImmediate"
        private const val MAX_RETRIES = 3
    }

    private val prefsManager = PreferencesManager(context)
    private val snapshotStorage = SnapshotStorage(context)
    private val repository = TimetableRepository(prefsManager)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker started (attempt ${runAttemptCount + 1})")

        // 1. Check network connectivity
        if (!isNetworkAvailable()) {
            Log.w(TAG, "No network available — aborting silently")
            return Result.success()
        }

        // 2. Check if credentials are saved
        if (!prefsManager.isLoggedIn()) {
            NotificationHelper.notifyAlert(
                context,
                "⚠️ TimetableTracker: Login Required",
                "Please open the app and log in to enable timetable monitoring."
            )
            return Result.success()
        }

        // 3. Fetch timetable from UPES portal
        val fetchResult = repository.fetchTimetableFull()

        if (fetchResult is com.timetabletracker.data.repository.Result.Error) {
            Log.e(TAG, "Fetch failed: ${fetchResult.message}")
            return handleFetchError(fetchResult.message)
        }

        val currentSlots = (fetchResult as com.timetabletracker.data.repository.Result.Success).data

        // 4. Load previous snapshot (only the timetable from the previous check)
        val previousSlots = snapshotStorage.loadPreviousSnapshot() ?: emptyList()

        // 5. Compare and flag changes (Green = Added, Red = Deleted, Muted-Orange = Rescheduled)
        val comparisonResult = TimetableComparator.compare(previousSlots, currentSlots)

        // 6. Save annotated display slots so the Home tab can render the highlighted table
        snapshotStorage.saveDisplaySlots(comparisonResult.displaySlots)

        // 7. Save current timetable snapshot (replaces previous timetable)
        snapshotStorage.saveSnapshot(currentSlots)

        // 8. Update last check time
        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        prefsManager.lastCheckTime = nowStr

        // 9. Send system notification if changes were detected
        if (comparisonResult.hasChanges) {
            if (prefsManager.notificationsEnabled) {
                NotificationHelper.notifyChanges(context, comparisonResult.summary)
            }
            Log.i(TAG, "Changes detected and notification sent")
        } else {
            Log.d(TAG, "No timetable changes detected")
        }

        return Result.success()
    }

    /**
     * Handles fetch errors with retries.
     */
    private fun handleFetchError(errorMessage: String): Result {
        val isAuthError = errorMessage.contains("401") ||
                errorMessage.contains("Login failed") ||
                errorMessage.contains("No credentials")

        return if (isAuthError) {
            NotificationHelper.notifyAlert(
                context,
                "🔐 TimetableTracker: Authentication Error",
                "Login failed. Please open the app and re-enter your credentials."
            )
            Result.failure()
        } else if (runAttemptCount < MAX_RETRIES - 1) {
            Log.w(TAG, "Retrying after failure (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
            Result.retry()
        } else {
            NotificationHelper.notifyAlert(
                context,
                "⚠️ TimetableTracker: Fetch Error",
                "Could not fetch timetable after $MAX_RETRIES attempts. Will retry at next scheduled time."
            )
            Result.failure()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
