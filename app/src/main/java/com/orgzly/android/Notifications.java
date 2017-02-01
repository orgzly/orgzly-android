package com.orgzly.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
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

        Intent resultIntent = new Intent(context, ShareActivity.class);
        resultIntent.setAction(Intent.ACTION_SEND);
        resultIntent.setType("text/plain");
        resultIntent.putExtra(Intent.EXTRA_TEXT, "");

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ShareActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Build notification */
        Notification notification =
                new NotificationCompat.Builder(context)
                        .setOngoing(true)
                        .setSmallIcon(R.drawable.cic_orgzly_notification)
                        .setContentTitle(context.getString(R.string.new_note))
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