package com.orgzly.android.ui.notifications;

import static com.orgzly.android.AppIntent.ACTION_ACCEPT_AND_STORE_REMOTE_HOST_KEY;
import static com.orgzly.android.AppIntent.ACTION_ACCEPT_REMOTE_HOST_KEY;
import static com.orgzly.android.AppIntent.ACTION_REJECT_REMOTE_HOST_KEY;
import static com.orgzly.android.NewNoteBroadcastReceiver.NOTE_TITLE;
import static com.orgzly.android.git.SshCredentialsProvider.ALLOW;
import static com.orgzly.android.git.SshCredentialsProvider.ALLOW_AND_STORE;
import static com.orgzly.android.git.SshCredentialsProvider.DENY;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.orgzly.android.AppIntent.ACTION_ACCEPT_AND_STORE_REMOTE_HOST_KEY;
import static com.orgzly.android.AppIntent.ACTION_ACCEPT_REMOTE_HOST_KEY;
import static com.orgzly.android.AppIntent.ACTION_REJECT_REMOTE_HOST_KEY;
import static com.orgzly.android.NewNoteBroadcastReceiver.NOTE_TITLE;
import static com.orgzly.android.git.SshCredentialsProvider.ALLOW;
import static com.orgzly.android.git.SshCredentialsProvider.ALLOW_AND_STORE;
import static com.orgzly.android.git.SshCredentialsProvider.DENY;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ActionReceiver;
import com.orgzly.android.AppIntent;
import com.orgzly.android.NewNoteBroadcastReceiver;
import com.orgzly.android.NotificationChannels;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.main.MainActivity;
import com.orgzly.android.ui.share.ShareActivity;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.ui.util.SystemServices;
import com.orgzly.android.util.LogUtils;

import org.eclipse.jgit.internal.transport.sshd.SshdText;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.URIish;

public class Notifications {
    public static final String TAG = Notifications.class.getName();

    public static final int ONGOING_NEW_NOTE_ID = 1;
    public static final int REMINDER_ID = 2;
    public static final int REMINDERS_SUMMARY_ID = 3;
    public static final int SYNC_IN_PROGRESS_ID = 4;
    public static final int SYNC_FAILED_ID = 5;
    public static final int SYNC_SSH_REMOTE_HOST_KEY = 6;

    public static final String REMINDERS_GROUP = "com.orgzly.notification.group.REMINDERS";

    public static void showOngoingNotification(Context context) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        PendingIntent newNotePendingIntent =
                ShareActivity.createNewNotePendingIntent(context, "ongoing notification", null);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.ONGOING)
                .setOngoing(true)
                .setSmallIcon(R.drawable.cic_logo_for_notification)
                .setContentTitle(context.getString(R.string.new_note))
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentText(context.getString(R.string.tap_to_create_new_note))
                .setContentIntent(newNotePendingIntent);

        builder.setPriority(
                getNotificationPriority(
                        AppPreferences.ongoingNotificationPriority(context)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PendingIntent quickNoteCreatePendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    new Intent(context, NewNoteBroadcastReceiver.class),
                    ActivityUtils.mutable(0));

            RemoteInput remoteInput = new RemoteInput.Builder(NOTE_TITLE)
                    .setLabel(context.getString(R.string.quick_note))
                    .build();

            // Add new note action
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                    R.drawable.ic_add, context.getString(R.string.quick_note), quickNoteCreatePendingIntent)
                    .addRemoteInput(remoteInput)
                    .build();
            builder.addAction(action);
        }

        // Add open action
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                ActivityUtils.mutable(PendingIntent.FLAG_UPDATE_CURRENT));
        builder.addAction(
                R.drawable.ic_open_in_new,
                context.getString(R.string.open),
                openAppPendingIntent);

        // Add sync action
        Intent syncIntent = new Intent(context, ActionReceiver.class);
        syncIntent.setAction(AppIntent.ACTION_SYNC_START);
        PendingIntent syncPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                syncIntent,
                ActivityUtils.mutable(PendingIntent.FLAG_UPDATE_CURRENT));
        builder.addAction(
                    R.drawable.ic_sync,
                    context.getString(R.string.sync),
                    syncPendingIntent);

        SystemServices.getNotificationManager(context).notify(ONGOING_NEW_NOTE_ID, builder.build());
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
        SystemServices.getNotificationManager(context).cancel(ONGOING_NEW_NOTE_ID);
    }

    /*
    Expandable notification to show when a Git sync repository server
    presents an unknown or unexpected SSH public key.
    Presents either two or three choices to the user. The selected action
    is passed back to the SshCredentialsProvider via a broadcast receiver.
    */
    public static void showSshRemoteHostKeyPrompt(Context context, URIish uri, CredentialItem... items) {
        // Parse CredentialItems
        List<String> messages = new ArrayList<>();
        List<CredentialItem.YesNoType> questions = new ArrayList<>();
        for (CredentialItem item : items) {
            messages.add(item.getPromptText());
            if (item instanceof CredentialItem.YesNoType) {
                questions.add((CredentialItem.YesNoType) item);
            }
        }
        String bigText = String.join("\n", messages.subList(1, messages.size()));

        // FIXME: The "modified key" prompt is too long to fit into a BigTextStyle notification.
        // Should we launch a dialog-themed activity from the notification?
        if (Objects.equals(questions.get(0).getPromptText(), SshdText.get().knownHostsModifiedKeyAcceptPrompt)) {
            // Remove SHA256 checksums (only show MD5)
            messages.remove(5);
            messages.remove(8);
            bigText = String.join("\n", messages.subList(3, messages.size() - 2));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.SYNC_PROMPT)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setSmallIcon(R.drawable.cic_logo_for_notification)
                .setColor(ContextCompat.getColor(context, R.color.notification))
                .setContentTitle(String.format("Accept public key for %s?", uri.getHost())) // TODO: strings.xml
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setContentText(messages.get(0))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(bigText));

        if (!questions.isEmpty()) {
            builder.addAction(
                    R.drawable.cic_logo_for_notification,
                    DENY,
                    PendingIntent.getBroadcast(
                            context,
                            0,
                            new Intent().setAction(ACTION_REJECT_REMOTE_HOST_KEY),
                            PendingIntent.FLAG_IMMUTABLE));
            // Middle button is only relevant when there are 2 questions
            if (questions.size() == 2) {
                builder.addAction(
                        R.drawable.cic_logo_for_notification,
                        ALLOW,
                        PendingIntent.getBroadcast(
                                context,
                                0,
                                new Intent().setAction(ACTION_ACCEPT_REMOTE_HOST_KEY),
                                PendingIntent.FLAG_IMMUTABLE));
            }
            builder.addAction(
                    R.drawable.cic_logo_for_notification,
                    ALLOW_AND_STORE,
                    PendingIntent.getBroadcast(
                            context,
                            0,
                            new Intent().setAction(ACTION_ACCEPT_AND_STORE_REMOTE_HOST_KEY),
                            PendingIntent.FLAG_IMMUTABLE));
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(SYNC_SSH_REMOTE_HOST_KEY, builder.build());
    }
}
