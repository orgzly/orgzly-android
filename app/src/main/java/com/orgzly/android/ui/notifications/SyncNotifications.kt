package com.orgzly.android.ui.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import com.orgzly.R
import com.orgzly.android.NotificationChannels
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.util.ActivityUtils.immutable
import com.orgzly.android.ui.util.getNotificationManager

object SyncNotifications {
    fun syncInProgressForegroundInfo(context: Context): ForegroundInfo {
        return ForegroundInfo(
            Notifications.SYNC_IN_PROGRESS_ID,
            createSyncInProgressNotification(context))
    }

    private fun createSyncInProgressNotification(context: Context): Notification {
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            immutable(PendingIntent.FLAG_UPDATE_CURRENT))

        val builder = NotificationCompat.Builder(context, NotificationChannels.SYNC_PROGRESS)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(context.getString(R.string.syncing_in_progress))
            .setColor(ContextCompat.getColor(context, R.color.notification))
            .setContentIntent(openAppPendingIntent)

        return builder.build()
    }

    fun showSyncFailedNotification(context: Context, text: String) {
        val openAppPendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                immutable(PendingIntent.FLAG_UPDATE_CURRENT))

        val builder = NotificationCompat.Builder(context, NotificationChannels.SYNC_FAILED)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.cic_logo_for_notification)
            .setContentTitle(context.getString(R.string.syncing_failed_title))
            .setColor(ContextCompat.getColor(context, R.color.notification))
            .setContentIntent(openAppPendingIntent)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))

        context.getNotificationManager().notify(Notifications.SYNC_FAILED_ID, builder.build())
    }

    fun cancelSyncFailedNotification(context: Context) {
        context.getNotificationManager().cancel(Notifications.SYNC_FAILED_ID)
    }
}