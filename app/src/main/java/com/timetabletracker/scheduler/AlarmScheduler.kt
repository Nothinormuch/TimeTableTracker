package com.timetabletracker.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.timetabletracker.data.local.PreferencesManager
import java.util.Calendar

/**
 * Manages scheduling of exact daily alarms using AlarmManager.
 * Alarm fires at the user-configured time and is rescheduled daily.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 100

    /**
     * Schedules (or reschedules) the daily timetable check alarm.
     * If the target time today has already passed, schedules for tomorrow.
     */
    fun scheduleDailyAlarm(context: Context) {
        val prefs = PreferencesManager(context)
        val hour = prefs.checkHour
        val minute = prefs.checkMinute

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildAlarmIntent(context)

        val triggerTime = getNextTriggerTime(hour, minute)
        Log.d(TAG, "Scheduling alarm for ${hour}:${minute.toString().padStart(2, '0')} → $triggerTime ms")

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intent
            )
            prefs.isMonitoringActive = true
            Log.i(TAG, "Daily alarm scheduled successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot set exact alarm — permission not granted", e)
        }
    }

    /**
     * Cancels the currently scheduled alarm.
     */
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildAlarmIntent(context)
        alarmManager.cancel(intent)
        PreferencesManager(context).isMonitoringActive = false
        Log.i(TAG, "Alarm cancelled")
    }

    /**
     * Returns true if an exact alarm can be scheduled on this device.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /**
     * Returns a human-readable string of the next scheduled alarm time.
     */
    fun getNextAlarmTimeString(context: Context): String {
        val prefs = PreferencesManager(context)
        if (!prefs.isMonitoringActive) return "Not scheduled"
        val hour = prefs.checkHour
        val minute = prefs.checkMinute
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "Daily at $displayHour:${minute.toString().padStart(2, '0')} $amPm"
    }

    private fun buildAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CHECK_TIMETABLE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
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
        // If the time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return calendar.timeInMillis
    }
}
