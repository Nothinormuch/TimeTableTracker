package com.timetabletracker.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timetabletracker.data.local.ChangeEvent
import com.timetabletracker.data.local.PreferencesManager
import com.timetabletracker.data.local.SnapshotStorage
import com.timetabletracker.data.repository.Result
import com.timetabletracker.data.repository.TimetableRepository
import com.timetabletracker.domain.TimetableComparator
import com.timetabletracker.notification.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager CoroutineWorker that performs the actual timetable check.
 * Enqueued by AlarmReceiver on each scheduled alarm.
 * Supports up to 3 retries (15 min apart) on network/API failure.
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
            return Result.success() // Don't retry — will run at next alarm
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

        // 3. Fetch timetable
        val fetchResult = repository.fetchTimetableFull()

        if (fetchResult is com.timetabletracker.data.repository.Result.Error) {
            Log.e(TAG, "Fetch failed: ${fetchResult.message}")
            return handleFetchError(fetchResult.message)
        }

        val currentSlots = (fetchResult as com.timetabletracker.data.repository.Result.Success).data

        // 4. Compare with previous snapshot
        val previousSlots = snapshotStorage.loadPreviousSnapshot() ?: emptyList()
        val comparisonResult = TimetableComparator.compare(previousSlots, currentSlots)

        // 5. Save new snapshot
        snapshotStorage.saveSnapshot(currentSlots)

        // 6. Update last check time
        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        prefsManager.lastCheckTime = nowStr

        // 7. Record history event
        val event = ChangeEvent(
            checkTime = nowStr,
            summary = if (comparisonResult.hasChanges) comparisonResult.summary else "No changes detected.",
            hasChanges = comparisonResult.hasChanges,
            changeDetails = comparisonResult.summary
        )
        snapshotStorage.appendHistory(event)

        // 8. Notify if changes found
        if (comparisonResult.hasChanges) {
            if (prefsManager.notificationsEnabled) {
                NotificationHelper.notifyChanges(context, comparisonResult.summary)
            }
            if (prefsManager.whatsappEnabled) {
                NotificationHelper.sendWhatsAppMessage(context, comparisonResult.summary)
            }
            Log.i(TAG, "Changes detected and notification sent")
        } else {
            Log.d(TAG, "No timetable changes detected")
        }

        return Result.success()
    }

    /**
     * Handles fetch errors by retrying up to MAX_RETRIES times (every 15 min).
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
