package com.orgzly.android.ui.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.NotificationChannels;
import com.orgzly.android.db.entity.BookAction;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncStatus;
import com.orgzly.android.ui.main.MainActivity;

import java.util.List;

import javax.inject.Inject;

public class SyncStatusBroadcastReceiver extends BroadcastReceiver {

    @Inject
    DataRepository dataRepository;

    @Override
    public void onReceive(Context context, Intent intent) {
        App.appComponent.inject(this);

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

    private void cancelSyncFailedNotification(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancel(Notifications.SYNC_FAILED_ID);
    }

    private void createSyncFailedNotification(Context context, SyncStatus status) {
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.SYNC_FAILED)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_logo_for_notification)
                .setContentTitle(context.getString(R.string.syncing_failed_title))
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentIntent(openAppPendingIntent);

        if (status.type == SyncStatus.Type.FAILED) {
            builder.setContentText(status.message);

        } else {
            List<BookView> books = dataRepository.getBooks(); // FIXME: ANR reported

            StringBuilder sb = new StringBuilder();
            for (BookView book: books) {
                BookAction action = book.getBook().getLastAction();
                if (action != null && action.getType() == BookAction.Type.ERROR) {
                    sb.append(book.getBook().getName())
                            .append(": ")
                            .append(action.getMessage())
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

        notificationManager.notify(Notifications.SYNC_FAILED_ID, builder.build());
    }
}
