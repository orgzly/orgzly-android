package com.orgzly.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.orgzly.R
import com.orgzly.android.reminders.ReminderNotifications


/**
 * Creates all channels for notifications.
 */
object NotificationChannels {

    const val ONGOING = "ongoing"
    const val REMINDERS = "reminders"
    const val SYNC_PROGRESS = "sync-progress"
    const val SYNC_FAILED = "sync-failed"

    @JvmStatic
    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createForOngoing(context)
            createForReminders(context)
            createForSyncProgress(context)
            createForSyncFailed(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createForReminders(context: Context) {
        val id = REMINDERS
        val name = context.getString(R.string.reminders_channel_name)
        val description = context.getString(R.string.reminders_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(id, name, importance)

        channel.description = description

        channel.enableLights(true)
        channel.lightColor = Color.BLUE

        channel.vibrationPattern = ReminderNotifications.VIBRATION_PATTERN

        channel.setShowBadge(false)

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createForOngoing(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val id = ONGOING
        val name = context.getString(R.string.ongoing_channel_name)
        val description = context.getString(R.string.ongoing_channel_description)
        val importance = NotificationManager.IMPORTANCE_MIN

        val channel = NotificationChannel(id, name, importance)

        channel.description = description

        channel.setShowBadge(false)

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createForSyncProgress(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val id = SYNC_PROGRESS
        val name = context.getString(R.string.sync_progress_channel_name)
        val description = context.getString(R.string.sync_progress_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(id, name, importance)

        channel.description = description

        channel.setShowBadge(false)

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createForSyncFailed(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val id = SYNC_FAILED
        val name = context.getString(R.string.sync_failed_channel_name)
        val description = context.getString(R.string.sync_failed_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val channel = NotificationChannel(id, name, importance)

        channel.description = description

        channel.setShowBadge(true)

        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(channel)
    }
}