package com.orgzly.android

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.orgzly.BuildConfig
import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.reminders.RemindersScheduler
import com.orgzly.android.ui.notifications.Notifications
import com.orgzly.android.ui.util.getNotificationManager
import com.orgzly.android.usecase.NoteUpdateStateDone
import com.orgzly.android.usecase.UseCaseRunner.run
import com.orgzly.android.util.LogUtils.d
import com.orgzly.android.util.async
import javax.inject.Inject

class NotificationBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var remindersScheduler: RemindersScheduler

    override fun onReceive(context: Context, intent: Intent) {
        App.appComponent.inject(this)

        if (BuildConfig.LOG_DEBUG) d(TAG, intent, intent.extras)

        async {
            dismissNotification(context, intent)

            when (intent.action) {
                AppIntent.ACTION_NOTE_MARK_AS_DONE -> {
                    val noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0)

                    run(NoteUpdateStateDone(noteId))
                }

                AppIntent.ACTION_REMINDER_SNOOZE_REQUESTED -> {
                    val noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0)
                    val noteTimeType = intent.getIntExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, 0)
                    val timestamp = intent.getLongExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0)

                    // Pass true as hasTime to use the alarm clock
                    remindersScheduler.scheduleSnoozeEnd(noteId, noteTimeType, timestamp, true)
                }
            }
        }
    }

    /**
     * If notification ID was passed as an extra,
     * it means this action was performed from the notification.
     * Cancel the notification here.
     */
    private fun dismissNotification(context: Context, intent: Intent) {
        val tag = intent.getStringExtra(AppIntent.EXTRA_NOTIFICATION_TAG)
        val id = intent.getIntExtra(AppIntent.EXTRA_NOTIFICATION_ID, 0)

        if (id > 0) {
            context.getNotificationManager().let {
                it.cancel(tag, id)
                cancelRemindersSummary(it)
            }
        }
    }

    private fun cancelRemindersSummary(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notifications = notificationManager.activeNotifications
            var reminders = 0
            for (notification in notifications) {
                if (notification.id == Notifications.REMINDER_ID) {
                    reminders++
                }
            }
            if (reminders == 0) {
                notificationManager.cancel(Notifications.REMINDERS_SUMMARY_ID)
            }
        }
    }

    companion object {
        val TAG: String = NotificationBroadcastReceiver::class.java.name
    }
}