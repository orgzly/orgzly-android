package com.orgzly.android.ui.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.NewNoteBroadcastReceiver;
import com.orgzly.android.NotificationChannels;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.main.MainActivity;
import com.orgzly.android.ui.share.ShareActivity;
import com.orgzly.android.util.LogUtils;

import static com.orgzly.android.NewNoteBroadcastReceiver.NOTE_TITLE;

public class Notifications {
    public static final String TAG = Notifications.class.getName();

    public static final int ONGOING_NEW_NOTE_ID = 1;
    public static final int REMINDER_ID = 2;
    public static final int REMINDERS_SUMMARY_ID = 3;
    public static final int SYNC_IN_PROGRESS_ID = 4;
    public static final int SYNC_FAILED_ID = 5;

    public static final String REMINDERS_GROUP = "com.orgzly.notification.group.REMINDERS";

    public static void createNewNoteNotification(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context);

        PendingIntent resultPendingIntent = ShareActivity.createNewNoteIntent(context, null);

        /* Build notification */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.ONGOING)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_logo_for_notification)
                .setContentTitle(context.getString(R.string.new_note))
                .setContentText(context.getString(R.string.tap_to_create_new_note))
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentIntent(resultPendingIntent);


        builder.setPriority(
                getNotificationPriority(
                        AppPreferences.ongoingNotificationPriority(context)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PendingIntent newNotePendingIntent = PendingIntent.getBroadcast(
                    context, 0, new Intent(context, NewNoteBroadcastReceiver.class), 0);

            RemoteInput remoteInput = new RemoteInput.Builder(NOTE_TITLE)
                    .setLabel(context.getString(R.string.quick_note))
                    .build();

            /* Add new note action */
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_add_white_24dp, context.getString(R.string.quick_note), newNotePendingIntent)
                    .addRemoteInput(remoteInput)
                    .build();
            builder.addAction(action);
        }

        /* Add open action */
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(
                R.drawable.ic_open_in_new_white_24dp,
                context.getString(R.string.open),
                openAppPendingIntent);

        /* Add sync action */
        Intent syncIntent = new Intent(context, SyncService.class);
        syncIntent.setAction(AppIntent.ACTION_SYNC_START);
        PendingIntent syncPendingIntent = PendingIntent.getService(
                context, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(
                    R.drawable.ic_sync_white_24dp,
                    context.getString(R.string.sync),
                    syncPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(ONGOING_NEW_NOTE_ID, builder.build());
    }

    private static int getNotificationPriority(String priority) {
        if ("max".equals(priority)) {
            return NotificationCompat.PRIORITY_MAX;

        } else if ("high".equals(priority)) {
            return NotificationCompat.PRIORITY_HIGH;

        } else if ("low".equals(priority)) {
            return NotificationCompat.PRIORITY_LOW;

        } else if ("min".equals(priority)) {
            return NotificationCompat.PRIORITY_MIN;
        }

        return NotificationCompat.PRIORITY_DEFAULT;
    }

    public static void cancelNewNoteNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(ONGOING_NEW_NOTE_ID);
    }

    private static BroadcastReceiver syncServiceReceiver = new SyncStatusBroadcastReceiver();

    public static Notification createSyncInProgressNotification(Context context) {
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.SYNC_PROGRESS)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_sync_white_24dp)
                .setContentTitle(context.getString(R.string.syncing_in_progress))
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentIntent(openAppPendingIntent);

        return builder.build();
    }

    public static void ensureSyncNotificationSetup(Context context) {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(context);
        bm.unregisterReceiver(syncServiceReceiver);
        bm.registerReceiver(syncServiceReceiver, new IntentFilter(AppIntent.ACTION_SYNC_STATUS));
    }
}