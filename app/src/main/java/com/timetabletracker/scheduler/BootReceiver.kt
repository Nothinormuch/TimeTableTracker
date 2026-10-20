package com.timetabletracker.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.timetabletracker.data.local.PreferencesManager

/**
 * BroadcastReceiver for BOOT_COMPLETED.
 * Re-registers the daily alarm after device reboot, since AlarmManager
 * alarms are cleared when the device reboots.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            return
        }

        Log.d(TAG, "Boot completed — checking if monitoring was active")

        val prefs = PreferencesManager(context)
        if (prefs.isMonitoringActive && prefs.isLoggedIn()) {
            Log.i(TAG, "Re-scheduling daily alarm after reboot")
            AlarmScheduler.scheduleDailyAlarm(context)
        } else {
            Log.d(TAG, "Monitoring was not active before reboot — skipping")
        }
    }
}
