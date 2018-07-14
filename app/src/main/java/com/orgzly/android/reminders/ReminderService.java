package com.orgzly.android.reminders;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.NotificationBroadcastReceiver;
import com.orgzly.android.NotificationChannels;
import com.orgzly.android.Notifications;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.TimesClient;
import com.orgzly.android.provider.views.DbTimeView;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.OrgFormatter;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;
import com.orgzly.org.datetime.OrgInterval;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Every event that can affect reminders is going through this service.
 */
public class ReminderService extends JobIntentService {
    public static final String TAG = ReminderService.class.getName();

    private static final String EXTRA_EVENT = "event";

    public static final int EVENT_DATA_CHANGED = 1;
    public static final int EVENT_JOB_TRIGGERED = 2;
    public static final int EVENT_SNOOZE_JOB_TRIGGERED = 3;
    public static final int EVENT_UNKNOWN = -1;

    static final int TIME_BEFORE_NOW = 1;
    static final int TIME_FROM_NOW = 2;

    public static final long[] SCHEDULED_NOTE_VIBRATE_PATTERN = {500, 50, 50, 300};

    public static List<NoteReminder> getNoteReminders(
            final Context context, final ReadableInstant now, final LastRun lastRun, final int beforeOrAfter) {

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        final List<NoteReminder> result = new ArrayList<>();

        TimesClient.forEachTime(context, noteTime -> {
            if (isRelevantNoteTime(context, noteTime)) {
                OrgDateTime orgDateTime = OrgDateTime.parse(noteTime.orgTimestampString);

                NoteReminderPayload payload = new NoteReminderPayload(
                        noteTime.noteId, noteTime.bookId, noteTime.bookName, noteTime.title, noteTime.timeType, orgDateTime);

                ReadableInstant[] interval = getInterval(beforeOrAfter, now, lastRun, noteTime.timeType);

                DateTime time = OrgDateTimeUtils.getFirstWarningTime(
                        noteTime.timeType,
                        orgDateTime,
                        interval[0],
                        interval[1],
                        new OrgInterval(9, OrgInterval.Unit.HOUR), // Default time of day
                        new OrgInterval(1, OrgInterval.Unit.DAY) // Warning period for deadlines
                );

                if (time != null) {
                    result.add(new NoteReminder(time, payload));
                }
            }
        });

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, "Fetched times, now sorting " + result.size() + " entries by time...");

        /* Sort by time, older first. */
        Collections.sort(result, (o1, o2) -> o1.getRunTime().compareTo(o2.getRunTime()));

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Times sorted, total " + result.size());

        return result;
    }

    private static boolean isRelevantNoteTime(Context context, TimesClient.NoteTime noteTime) {
        /* Not done-type state. */
        Set<String> doneStateKeywords = AppPreferences.doneKeywordsSet(context);
        boolean state = noteTime.state == null || !doneStateKeywords.contains(noteTime.state);

        boolean scheduled = AppPreferences.remindersForScheduledEnabled(context) && noteTime.timeType == DbTimeView.SCHEDULED_TIME;
        boolean deadline = AppPreferences.remindersForDeadlineEnabled(context) && noteTime.timeType == DbTimeView.DEADLINE_TIME;

        return state && (scheduled || deadline);
    }

    /**
     * Notify reminder service about changes that might affect scheduling of reminders.
     */
    public static void notifyDataChanged(Context context) {
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(ReminderService.EXTRA_EVENT, ReminderService.EVENT_DATA_CHANGED);
        enqueueWork(context, intent);
    }

    /**
     * Notify ReminderService about the triggered job.
     */
    public static void notifyJobTriggered(Context context) {
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(ReminderService.EXTRA_EVENT, ReminderService.EVENT_JOB_TRIGGERED);
        enqueueWork(context, intent);
    }

    public static void notifySnoozeTriggered(Context context, long noteId, int noteTimeType, long timestamp) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(ReminderService.EXTRA_EVENT, ReminderService.EVENT_SNOOZE_JOB_TRIGGERED);
        intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId);
        intent.putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType);
        intent.putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp);
        enqueueWork(context, intent);
    }

    private static void enqueueWork(Context context, Intent intent) {
        ReminderService.enqueueWork(context, ReminderService.class, App.REMINDER_SERVICE_JOB_ID, intent);
    }

    private static ReadableInstant[] getInterval(int beforeOrAfter, ReadableInstant now, LastRun lastRun, int timeType) {
        ReadableInstant[] res = new ReadableInstant[2];

        switch (beforeOrAfter) {
            case TIME_BEFORE_NOW: // Before now, starting from lastRun, depending on timeType
                res[0] = timeType == DbTimeView.SCHEDULED_TIME ? lastRun.scheduled : lastRun.deadline;
                if (res[0] == null) {
                    res[0] = now;
                }
                res[1] = now;
                break;

            case TIME_FROM_NOW:
                res[0] = now;
                res[1] = null;
                break;

            default:
                throw new IllegalArgumentException("Before or after now?");
        }

        return res;
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        if (!AppPreferences.remindersForScheduledEnabled(this) && !AppPreferences.remindersForDeadlineEnabled(this)) {
            showScheduledAtNotification("Reminders are disabled");
            return;
        }

        DateTime now = new DateTime();

        LastRun lastRun = readLastRun();

        int event = intent.getIntExtra(EXTRA_EVENT, EVENT_UNKNOWN);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Event: " + event);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, " Last: " + lastRun);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "  Now: " + now);

        switch (event) {
            case EVENT_DATA_CHANGED:
                onDataChanged(now, lastRun);
                break;

            case EVENT_JOB_TRIGGERED:
                onJobTriggered(now, lastRun);
                break;

            case EVENT_SNOOZE_JOB_TRIGGERED:
                long noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0);
                int noteTimeType = intent.getIntExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, 0);
                long timestamp = intent.getLongExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0);
                if (noteId > 0) {
                    onSnoozeTriggered(this, noteId, noteTimeType, timestamp);
                }
                break;

            default:
                Log.e(TAG, "Unknown event received, ignoring it");
                return;
        }

        writeLastRun(now);
    }

    private LastRun readLastRun() {
        LastRun lastRun = new LastRun();
        long ms;

        ms = AppPreferences.reminderLastRunForScheduled(this);
        if (ms > 0) {
            lastRun.scheduled = new DateTime(ms);
        }

        ms = AppPreferences.reminderLastRunForDeadline(this);
        if (ms > 0) {
            lastRun.deadline = new DateTime(ms);
        }

        return lastRun;
    }

    private void writeLastRun(DateTime now) {
        Context context = this;

        if (AppPreferences.remindersForScheduledEnabled(context)) {
            AppPreferences.reminderLastRunForScheduled(context, now.getMillis());
        }

        if (AppPreferences.remindersForDeadlineEnabled(context)) {
            AppPreferences.reminderLastRunForDeadline(context, now.getMillis());
        }
    }

    /**
     * Schedule the next job for times after last run.
     */
    private void onDataChanged(DateTime now, LastRun lastRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, now, lastRun);

        ReminderJob.cancelAll();

        scheduleNextJob(now, lastRun);
    }

    private void scheduleNextJob(DateTime now, LastRun lastRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, now, lastRun);

        List<NoteReminder> notes = ReminderService.getNoteReminders(
                this, now, lastRun, TIME_FROM_NOW);

        String log;

        if (!notes.isEmpty()) {
            /* Schedule only the first upcoming time. */
            NoteReminder firstNote = notes.get(0);

            /* Schedule *in* exactMs. */
            long exactMs = firstNote.getRunTime().getMillis() - now.getMillis();
            if (exactMs < 0) {
                exactMs = 1;
            }

            int jobId = ReminderJob.scheduleJob(exactMs);

            log = "#" + jobId + " @ " + jobRunTimeString(jobId) + " for “" + firstNote.getPayload().title + "”";

        } else {
            log = "No notes found";
        }

        showScheduledAtNotification(log);
    }

    private String jobRunTimeString(int jobId) {
        long jobTime = getJobRunTime(jobId);
        DateTime dateTime = new DateTime(jobTime);
        return DateTimeFormat.mediumDateTime().print(dateTime);
    }

    /**
     * Display reminders for all notes with times between
     * last run and now. Then schedule the next job for times after now.
     */
    private void onJobTriggered(DateTime now, LastRun lastRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, now, lastRun);

        ReminderJob.cancelAll();

        String msg;

        if (lastRun != null) {
            /* Show notifications for all notes with times from previous run until now. */
            List<NoteReminder> notes = ReminderService.getNoteReminders(
                    this, now, lastRun, TIME_BEFORE_NOW);

            if (!notes.isEmpty()) {
                msg = "Found " + notes.size() + " notes between " + lastRun + " and " + now;
                showNotification(this, notes);
            } else {
                msg = "No notes found between " + lastRun + " and " + now;
            }
        } else {
            msg = "No previous run";
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg);

        /* Schedule from now. */
        scheduleNextJob(now, lastRun);
    }

    private void onSnoozeTriggered(final Context context, final long noteId,
                                   final int noteTimeType, final long timestamp) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);

        String msg;
        // FIXME TODO replace this with a simpler query, may need to create it in notesclient
        final List<NoteReminder> result = new ArrayList<>();
        TimesClient.forEachTime(context, noteTime -> {
            if (noteTime.noteId == noteId && noteTime.timeType == noteTimeType && isRelevantNoteTime(context, noteTime)) {
                OrgDateTime orgDateTime = OrgDateTime.parse(noteTime.orgTimestampString);
                NoteReminderPayload payload = new NoteReminderPayload(
                        noteTime.noteId,
                        noteTime.bookId,
                        noteTime.bookName,
                        noteTime.title,
                        noteTime.timeType,
                        orgDateTime);
                DateTime timestampDateTime = new DateTime(timestamp);
                result.add(new NoteReminder(timestampDateTime, payload));
            }
        });

        if (!result.isEmpty()) {
            msg = "Found " + result.size() + " notes";
            showNotification(this, result);
        } else {
            msg = "No notes found";
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg);
    }

    private long getJobRunTime(int jobId) {
        JobRequest jobRequest = JobManager.instance().getJobRequest(jobId);
        return jobRequest.getScheduledAt() + jobRequest.getStartMs();
    }

    private void showNotification(Context context, List<NoteReminder> notes) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context, notes);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        for (int i = 0; i < notes.size(); i++) {
            NoteReminder noteReminder = notes.get(i);

            String notificationTag = String.valueOf(noteReminder.getPayload().noteId);
            int notificationId = Notifications.REMINDER;

            String line = context.getString(
                    noteReminder.getPayload().timeType == DbTimeView.SCHEDULED_TIME ? R.string.reminder_for_scheduled : R.string.reminder_for_deadline,
                    noteReminder.getPayload().orgDateTime.toStringWithoutBrackets());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setColor(ContextCompat.getColor(context, R.color.notification))
                    .setSmallIcon(R.drawable.ic_logo_for_notification);

            NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();

            /* Set vibration. */
            if (AppPreferences.remindersVibrate(context)) {
                builder.setVibrate(SCHEDULED_NOTE_VIBRATE_PATTERN);
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
            String doneActionText = noteReminder.getPayload().orgDateTime.hasRepeater() ?
                    getString(R.string.mark_as_done_with_repeater, noteReminder.getPayload().orgDateTime.getRepeater().toString()) :
                    getString(R.string.mark_as_done);

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
            String reminderSnoozeActionText = getString(R.string.reminder_snooze);

            long timestamp = noteReminder.getRunTime().getMillis();
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, timestamp);
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
    }

    /**
     * Display notification on every attempt to schedule a reminder.
     * Used for debugging.
     */
    private void showScheduledAtNotification(String msg) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg);

//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
//                .setCategory(NotificationCompat.CATEGORY_REMINDER)
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setColor(ContextCompat.getColor(this, R.color.notification))
//                .setSmallIcon(R.drawable.cic_orgzly_notification)
//                .setContentTitle("Next reminder")
//                .setContentText(msg);
//
//        notificationManager.notify(Notifications.REMINDER_SCHEDULED, builder.build());
    }

    private PendingIntent markNoteAsDonePendingIntent(
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

    private PendingIntent reminderSnoozePendingIntent(Context context,
                                                      long noteId,
                                                      int noteTimeType, // scheduled vs deadline
                                                      long timestamp,
                                                      String notificationTag,
                                                      int notificationId) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
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

    static class LastRun {
        DateTime scheduled;
        DateTime deadline;

        @Override
        public String toString() {
            return "Scheduled: " + scheduled + "  Deadline: " + deadline;
        }
    }
}
