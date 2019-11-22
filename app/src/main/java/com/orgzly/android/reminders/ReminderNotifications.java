package com.orgzly.android.reminders;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.NotificationBroadcastReceiver;
import com.orgzly.android.NotificationChannels;
import com.orgzly.android.db.dao.ReminderTimeDao;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.ui.notifications.Notifications;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.OrgFormatter;

import java.util.List;

public class ReminderNotifications {

    public static final long[] VIBRATION_PATTERN = { 500, 50, 50, 300 };

    static void showNotification(Context context, List<NoteReminder> notes) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        for (int i = 0; i < notes.size(); i++) {
            NoteReminder noteReminder = notes.get(i);

            String notificationTag = String.valueOf(noteReminder.getPayload().noteId);
            int notificationId = Notifications.REMINDER_ID;

            @StringRes int contentResId;
            if (noteReminder.getPayload().timeType == ReminderTimeDao.SCHEDULED_TIME) {
                contentResId = R.string.reminder_for_scheduled;
            } else if (noteReminder.getPayload().timeType == ReminderTimeDao.DEADLINE_TIME) {
                contentResId = R.string.reminder_for_deadline;
            } else {
                contentResId = R.string.reminder_for_event;
            }

            String line = context.getString(
                    contentResId, noteReminder.getPayload().orgDateTime.toStringWithoutBrackets());

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                            .setAutoCancel(true)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setColor(ContextCompat.getColor(context, R.color.notification))
                            .setSmallIcon(R.drawable.ic_logo_for_notification);

            if (groupReminders()) {
                builder.setGroup(Notifications.REMINDERS_GROUP);
            }

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

            /* Set vibration. */
            if (AppPreferences.remindersVibrate(context)) {
                builder.setVibrate(VIBRATION_PATTERN);
            }

            /* Set notification sound. */
            if (AppPreferences.remindersSound(context)) {
                Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                builder.setSound(sound);
            }

            /* Set LED. */
            if (AppPreferences.remindersLed(context)) {
                builder.setLights(Color.BLUE, 1000, 5000);
            }

            builder.setContentTitle(OrgFormatter.parse(
                    noteReminder.getPayload().title,
                    context,
                    false, // Do not linkify links in notification,
                    false // Do not parse checkboxes
            ));

            builder.setContentText(line);

            builder.setStyle(new NotificationCompat.InboxStyle()
                    .setSummaryText(noteReminder.getPayload().bookName)
                    .addLine(line)
            );

            /* Open note on notification click. */
            PendingIntent openPi = ActivityUtils.mainActivityPendingIntent(
                    context, noteReminder.getPayload().bookId, noteReminder.getPayload().noteId);
            builder.setContentIntent(openPi);

            /* Mark as done action - text depends on repeater. */
            String doneActionText = noteReminder.getPayload().orgDateTime.hasRepeater()
                    ? context.getString(R.string.mark_as_done_with_repeater, noteReminder.getPayload().orgDateTime.getRepeater().toString())
                    : context.getString(R.string.mark_as_done);

            NotificationCompat.Action markAsDoneAction =
                    new NotificationCompat.Action(R.drawable.ic_done_white_24dp,
                            doneActionText,
                            markNoteAsDonePendingIntent(
                                    context,
                                    noteReminder.getPayload().noteId,
                                    notificationTag,
                                    notificationId));
            builder.addAction(markAsDoneAction);
            wearableExtender.addAction(markAsDoneAction);

            /* Snooze action. */
            String reminderSnoozeActionText = context.getString(R.string.reminder_snooze);

            long timestamp = noteReminder.getRunTime().getMillis();

            NotificationCompat.Action reminderSnoozeAction =
                    new NotificationCompat.Action(R.drawable.ic_snooze_white_24dp,
                            reminderSnoozeActionText,
                            reminderSnoozePendingIntent(
                                    context,
                                    noteReminder.getPayload().noteId,
                                    noteReminder.getPayload().timeType,
                                    timestamp,
                                    notificationTag,
                                    notificationId));
            builder.addAction(reminderSnoozeAction);
            wearableExtender.addAction(reminderSnoozeAction);

            builder.extend(wearableExtender);

            notificationManager.notify(notificationTag, notificationId, builder.build());
        }

        // Create a group summary notification, but only if notifications can be grouped
        if (groupReminders()) {
            if (notes.size() > 0) {
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                                .setAutoCancel(true)
                                .setSmallIcon(R.drawable.ic_logo_for_notification)
                                .setGroup(Notifications.REMINDERS_GROUP)
                                .setGroupSummary(true);

                notificationManager.notify(Notifications.REMINDERS_SUMMARY_ID, builder.build());
            }
        }
    }


    // Create a group summary notification, but only if notifications be grouped and expanded
    private static boolean groupReminders() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }


    private static PendingIntent markNoteAsDonePendingIntent(
            Context context, long noteId, String notificationTag, int notificationId) {
        Intent intent = new Intent(context, NotificationBroadcastReceiver.class);

        intent.setAction(AppIntent.ACTION_NOTE_MARK_AS_DONE);

        intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId);

        intent.putExtra(AppIntent.EXTRA_NOTIFICATION_TAG, notificationTag);
        intent.putExtra(AppIntent.EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getBroadcast(
                context,
                Long.valueOf(noteId).intValue(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent reminderSnoozePendingIntent(
            Context context,
            long noteId,
            int noteTimeType,
            long timestamp,
            String notificationTag,
            int notificationId) {

        Intent intent = new Intent(context, NotificationBroadcastReceiver.class);

        intent.setAction(AppIntent.ACTION_REMINDER_SNOOZE_REQUEST);

        intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId);
        intent.putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType);
        intent.putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp);
        intent.putExtra(AppIntent.EXTRA_NOTIFICATION_TAG, notificationTag);
        intent.putExtra(AppIntent.EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getBroadcast(
                context,
                Long.valueOf(noteId).intValue(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
