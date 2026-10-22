package com.timetabletracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.timetabletracker.MainActivity
import com.timetabletracker.R

/**
 * Handles creating notification channels and posting notifications.
 */
object NotificationHelper {

    private const val CHANNEL_CHANGES_ID = "TIMETABLE_CHANGES"
    private const val CHANNEL_CHANGES_NAME = "Timetable Changes"
    private const val CHANNEL_ALERTS_ID = "TIMETABLE_ALERTS"
    private const val CHANNEL_ALERTS_NAME = "App Alerts"

    private const val NOTIFICATION_ID_CHANGES = 1001
    private const val NOTIFICATION_ID_ALERT = 1002

    /**
     * Creates all notification channels. Call this once at app startup.
     */
    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val changesChannel = NotificationChannel(
            CHANNEL_CHANGES_ID,
            CHANNEL_CHANGES_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for timetable changes detected"
            enableVibration(true)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            CHANNEL_ALERTS_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "App status alerts (auth errors, API errors)"
        }

        nm.createNotificationChannel(changesChannel)
        nm.createNotificationChannel(alertsChannel)
    }

    /**
     * Posts a notification about detected timetable changes.
     */
    fun notifyChanges(context: Context, summary: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHANGES_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_title_changed))
            .setContentText(summary.lines().firstOrNull() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_CHANGES, notification)
    }

    /**
     * Posts an alert notification (auth failure, API error, etc.)
     */
    fun notifyAlert(context: Context, title: String, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID_ALERT, notification)
    }
}
