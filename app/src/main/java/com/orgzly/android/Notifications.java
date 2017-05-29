package com.orgzly.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.ShareActivity;
import com.orgzly.android.util.LogUtils;

public class Notifications {
    public static final String TAG = Notifications.class.getName();

    public final static int ONGOING_NEW_NOTE = 1;
    public final static int REMINDER = 2;

    public static void createNewNoteNotification(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context);

        PendingIntent resultPendingIntent = ShareActivity.createNewNoteIntent(context);

        /* Build notification */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setOngoing(true)
                .setSmallIcon(R.drawable.cic_orgzly_notification)
                .setContentTitle(context.getString(R.string.new_note))
                .setContentText(context.getString(R.string.tap_to_create_new_note))
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentIntent(resultPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN); // Don't show icon on status bar

        /* add sync action */
        Intent syncIntent = new Intent(context, SyncService.class);
        syncIntent.setAction(AppIntent.ACTION_SYNC_START);

        PendingIntent syncPendingIntent = PendingIntent.getService(context, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(
                    R.drawable.ic_sync_white_24dp,
                    context.getString(R.string.sync),
                    syncPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(ONGOING_NEW_NOTE, builder.build());
    }

    public static void cancelNewNoteNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(ONGOING_NEW_NOTE);
    }

}