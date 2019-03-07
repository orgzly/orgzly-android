package com.orgzly.android;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import com.orgzly.BuildConfig;
import com.orgzly.android.reminders.SnoozeJob;
import com.orgzly.android.ui.notifications.Notifications;
import com.orgzly.android.usecase.NoteUpdateStateDone;
import com.orgzly.android.usecase.UseCaseRunner;
import com.orgzly.android.util.LogUtils;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class NotificationActionService extends JobIntentService {
    public static final String TAG = NotificationActionService.class.getName();

    public static void enqueueWork(Context context, Intent intent) {
        NotificationActionService.enqueueWork(
                context,
                NotificationActionService.class,
                App.NOTIFICATION_SERVICE_JOB_ID,
                intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        dismissNotification(this, intent);

        if (AppIntent.ACTION_NOTE_MARK_AS_DONE.equals(intent.getAction())) {
            long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0);

            if (noteId > 0) {
                UseCaseRunner.run(new NoteUpdateStateDone(noteId));
            } else {
                throw new IllegalArgumentException("Missing note ID");
            }

        } else if (AppIntent.ACTION_REMINDER_SNOOZE_REQUEST.equals(intent.getAction())) {
            long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0);
            int noteTimeType = intent.getIntExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, 0);
            long timestamp = intent.getLongExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0);
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
            if (noteId > 0) {
                SnoozeJob.scheduleJob(this, noteId, noteTimeType, timestamp);
            } else {
                throw new IllegalArgumentException("Missing note id");
            }
        }
    }

    /**
     * If notification ID was passed as an extra,
     * it means this action was performed from the notification.
     * Cancel the notification here.
     */
    private void dismissNotification(Context context, Intent intent) {
        int id = intent.getIntExtra(AppIntent.EXTRA_NOTIFICATION_ID, 0);
        String tag = intent.getStringExtra(AppIntent.EXTRA_NOTIFICATION_TAG);

        if (id > 0) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(tag, id);

            cancelRemindersSummary(notificationManager);
        }
    }

    private void cancelRemindersSummary(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();

            int reminders = 0;

            for (StatusBarNotification notification: notifications) {
                if (notification.getId() == Notifications.REMINDER_ID) {
                    reminders++;
                }
            }

            if (reminders == 0) {
                notificationManager.cancel(Notifications.REMINDERS_SUMMARY_ID);
            }
        }
    }
}
