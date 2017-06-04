package com.orgzly.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.sync.SyncStatus;
import com.orgzly.android.ui.MainActivity;
import com.orgzly.android.ui.ShareActivity;
import com.orgzly.android.util.LogUtils;

import java.util.List;

public class Notifications {
    public static final String TAG = Notifications.class.getName();

    public final static int ONGOING_NEW_NOTE = 1;
    public final static int REMINDER = 2;
    public final static int SYNC_IN_PROGRESS = 3;
    public final static int SYNC_FAILED = 4;

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


    private static BroadcastReceiver syncServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SyncStatus status = SyncStatus.fromIntent(intent);

            switch (status.type) {
                case STARTING:
                    cancelSyncFailedNotification(context);
                    break;
                case CANCELED:
                case FAILED:
                case NOT_RUNNING:
                case FINISHED:
                    if (AppPreferences.showSyncNotifications(context)) {
                        createSyncFailedNotification(context, status);
                    }
                    break;
            }
        }
    };

    public static Notification createSyncInProgressNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_sync_white_24dp)
                .setContentTitle(context.getString(R.string.syncing_in_progress))
                .setColor(ContextCompat.getColor(context, R.color.notification));

        return builder.build();
    }

    private static void createSyncFailedNotification(Context context, SyncStatus status) {
        PendingIntent openOrgzlyPendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.cic_orgzly_notification)
                .setContentTitle(context.getString(R.string.syncing_failed_title))
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentIntent(openOrgzlyPendingIntent);

        if (status.type == SyncStatus.Type.FAILED) {
            builder.setContentText(status.message);
        } else {
            List<Book> books = new Shelf(context).getBooks();

            StringBuilder sb = new StringBuilder();
            for (Book book: books) {
                if (book.getLastAction().getType() == BookAction.Type.ERROR) {
                    sb.append(book.getName())
                            .append(": ")
                            .append(book.getLastAction().getMessage())
                            .append("\n");
                }
            }

            String message = sb.toString().trim();

            if (message.length() == 0) {
                /* no error, don't show the notification */
                return;
            }

            builder.setContentText(message);
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(SYNC_FAILED, builder.build());
    }

    private static void cancelSyncFailedNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(SYNC_FAILED);
    }

    public static void ensureSyncNotificationSetup(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(syncServiceReceiver);

        LocalBroadcastManager.getInstance(context)
                .registerReceiver(syncServiceReceiver, new IntentFilter(AppIntent.ACTION_SYNC_STATUS));
    }
}