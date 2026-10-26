package com.timetabletracker.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.util.Log
import com.timetabletracker.R
import com.timetabletracker.data.local.PreferencesManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manages exact timetable-check alarms. Interval schedules keep a rolling
 * 24-hour set of alarms anchored to the user's selected first-check time.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE_BASE = 100
    private const val MAX_ALARMS = 24
    private val supportedIntervals = setOf(1, 2, 5, 24)

    /**
     * Replaces the current alarm set. Reset the anchor after the user changes
     * the selected time or repeat interval.
     */
    fun scheduleAlarms(context: Context, resetAnchor: Boolean = false): Boolean {
        val prefs = PreferencesManager(context)
        val intervalHours = prefs.checkIntervalHours
        if (intervalHours !in supportedIntervals) {
            Log.e(TAG, "Unsupported check interval: $intervalHours hours")
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val triggerTimes = if (intervalHours == 24) {
            listOf(getNextTriggerTime(prefs.checkHour, prefs.checkMinute))
        } else {
            val intervalMillis = TimeUnit.HOURS.toMillis(intervalHours.toLong())
            val anchor = when {
                resetAnchor || prefs.scheduleAnchorTime == 0L ->
                    getNextTriggerTime(prefs.checkHour, prefs.checkMinute)
                        .also { prefs.scheduleAnchorTime = it }
                else -> prefs.scheduleAnchorTime
            }
            val elapsedIntervals = if (now < anchor) {
                0L
            } else {
                (now - anchor) / intervalMillis + 1
            }
            var nextTime = anchor + elapsedIntervals * intervalMillis
            val horizon = now + TimeUnit.HOURS.toMillis(24)
            buildList {
                while (nextTime < horizon && size < MAX_ALARMS) {
                    add(nextTime)
                    nextTime += intervalMillis
                }
            }
        }

        try {
            cancelScheduledAlarms(context, alarmManager)
            triggerTimes.forEachIndexed { index, triggerTime ->
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    buildAlarmIntent(context, index)
                )
            }
            prefs.isMonitoringActive = true
            Log.i(
                TAG,
                "Scheduled ${triggerTimes.size} check alarm(s), interval=$intervalHours hour(s)"
            )
            return true
        } catch (e: SecurityException) {
            cancelScheduledAlarms(context, alarmManager)
            prefs.isMonitoringActive = false
            Log.e(TAG, "Cannot set exact alarm — permission not granted", e)
            return false
        }
    }

    /**
     * Cancels every alarm in the rolling schedule.
     */
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelScheduledAlarms(context, alarmManager)
        PreferencesManager(context).isMonitoringActive = false
        Log.i(TAG, "Check alarms cancelled")
    }

    /**
     * Returns true if an exact alarm can be scheduled on this device.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * Returns a human-readable description of the next scheduled check.
     */
    fun getNextAlarmTimeString(context: Context): String {
        val prefs = PreferencesManager(context)
        if (!prefs.isMonitoringActive) return "Not scheduled"

        val nextTriggerTime = if (prefs.checkIntervalHours == 24) {
            getNextTriggerTime(prefs.checkHour, prefs.checkMinute)
        } else {
            val intervalMillis = TimeUnit.HOURS.toMillis(prefs.checkIntervalHours.toLong())
            val anchor = if (prefs.scheduleAnchorTime == 0L) {
                getNextTriggerTime(prefs.checkHour, prefs.checkMinute)
            } else {
                prefs.scheduleAnchorTime
            }
            val now = System.currentTimeMillis()
            val elapsedIntervals = if (now < anchor) {
                0L
            } else {
                (now - anchor) / intervalMillis + 1
            }
            anchor + elapsedIntervals * intervalMillis
        }

        val time = DateFormat.getTimeFormat(context).format(nextTriggerTime)
        return if (prefs.checkIntervalHours == 24) {
            context.getString(R.string.next_check_daily, time)
        } else {
            val unit = context.getString(
                if (prefs.checkIntervalHours == 1) R.string.hour_singular else R.string.hour_plural
            )
            context.getString(R.string.next_check_interval, time, prefs.checkIntervalHours, unit)
        }
    }

    private fun cancelScheduledAlarms(context: Context, alarmManager: AlarmManager) {
        repeat(MAX_ALARMS) { index ->
            alarmManager.cancel(buildAlarmIntent(context, index))
        }
    }

    private fun buildAlarmIntent(context: Context, index: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CHECK_TIMETABLE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getNextTriggerTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
