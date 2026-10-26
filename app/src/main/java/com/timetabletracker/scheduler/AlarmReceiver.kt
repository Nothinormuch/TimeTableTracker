package com.timetabletracker.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.timetabletracker.worker.TimetableFetchWorker
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver triggered by each scheduled AlarmManager check.
 * Enqueues a one-off WorkManager job and refreshes the rolling alarm set.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_TIMETABLE = "com.timetabletracker.ACTION_CHECK_TIMETABLE"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CHECK_TIMETABLE) return

        Log.d(TAG, "Alarm received — enqueuing timetable fetch worker")

        // Enqueue the fetch worker with network constraint and backoff
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TimetableFetchWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.MINUTES
            )
            .addTag(TimetableFetchWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // Refill the rolling set of interval alarms.
        AlarmScheduler.scheduleAlarms(context)
    }
}
