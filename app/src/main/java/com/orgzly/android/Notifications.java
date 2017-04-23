package com.orgzly.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ui.ShareActivity;
import com.orgzly.android.util.LogUtils;

public class Notifications {
    public static final String TAG = Notifications.class.getName();

    private final static int NEW_NOTE_NOTIFICATION_ID = 1;

    public static void createNewNoteNotification(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context);

        PendingIntent resultPendingIntent = ShareActivity.createNewNoteIntent(context);

        /* Build notification */
        Notification notification =
                new NotificationCompat.Builder(context)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.cic_orgzly_notification)
                        .setContentTitle(context.getString(R.string.new_note))
                        .setContentText(context.getString(R.string.tap_to_create_new_note))
                        .setColor(ContextCompat.getColor(context, R.color.notification))
                        .setContentIntent(resultPendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_MIN) // Don't show icon on status bar
                        .build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NEW_NOTE_NOTIFICATION_ID, notification);
    }

    public static void cancelNewNoteNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(NEW_NOTE_NOTIFICATION_ID);
    }
}