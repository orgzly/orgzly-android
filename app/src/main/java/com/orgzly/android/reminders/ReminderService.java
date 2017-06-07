package com.orgzly.android.reminders;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.ActionService;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Notifications;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.ui.util.ActivityUtils;
import com.orgzly.android.util.LogUtils;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Everything related to reminders goes through this service.
 *
 * FIXME: Work in progress, logic below is incomplete and/or broken.
 */
public class ReminderService extends IntentService {
    public static final String TAG = ReminderService.class.getName();

    public static final String EXTRA_EVENT = "event";

    public static final int EVENT_DATA_CHANGED = 1;
    public static final int EVENT_JOB_TRIGGERED = 2;
    public static final int EVENT_UNKNOWN = -1;

    private static final long[] SCHEDULED_NOTE_VIBRATE_PATTERN = new long[] { 500, 50, 50, 300 };

    public ReminderService() {
        super(TAG);

        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        if (! AppPreferences.remindersForScheduledTimes(this)) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Reminders are disabled");
            return;
        }

        DateTime now = new DateTime();

        /* Previous run time. */
        DateTime prevRun = null;
        long prevRunMillis = AppPreferences.reminderServiceLastRun(this);
        if (prevRunMillis > 0) {
            prevRun = new DateTime(prevRunMillis);
        }

        int event = intent.getIntExtra(EXTRA_EVENT, EVENT_UNKNOWN);


        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Event: " + event);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, " Prev: " + prevRun);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "  Now: " + now);

        switch (event) {
            case EVENT_DATA_CHANGED:
                onDataChanged(now, prevRun);
                break;

            case EVENT_JOB_TRIGGERED:
                onJobTriggered(now, prevRun);
                break;

            default:
                Log.e(TAG, "Unknown event received, ignoring it");
                return;
        }

        AppPreferences.reminderServiceLastRun(this, now.getMillis());
    }

    /**
     * Schedule the next job for times after last run.
     */
    private void onDataChanged(DateTime now, DateTime prevRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, now, prevRun);

        ReminderJob.cancelAll();

        DateTime fromTime = prevRun;
        if (prevRun == null) {
            fromTime = now;
        }

        scheduleNextJob(now, fromTime);
    }

    private void scheduleNextJob(DateTime now, DateTime fromTime) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, now, fromTime);

        List<NoteWithTime> notes = ReminderService.getNotesWithTimeInInterval(this, fromTime, null);

        if (! notes.isEmpty()) {
            /* Schedule only the first upcoming time. */
            NoteWithTime firstNote = notes.get(0);

            /* Schedule *in* exactMs. */
            long exactMs = firstNote.time.getMillis() - now.getMillis();
            if (exactMs < 0) {
                exactMs = 1;
            }

            int jobId = ReminderJob.scheduleJob(exactMs);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG,
                    "Scheduled for " + jobRunTimeString(jobId) + " (" + firstNote.title + ")");

            AppPreferences.reminderServiceJobId(this, jobId);

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "No notes found after " + fromTime);
        }
    }

    private String jobRunTimeString(int jobId) {
        long jobTime = getJobRunTime(jobId);
        DateTime dateTime = new DateTime(jobTime);
        return dateTime.toString();
    }

    /**
     * Display reminders for all notes with times between
     * last run and now. Then schedule the next job for times after now.
     */
    private void onJobTriggered(DateTime now, DateTime prevRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, now, prevRun);

        ReminderJob.cancelAll();

        if (prevRun != null) {
            /* Show notifications for all notes with times from previous run until now. */
            List<NoteWithTime> notes = ReminderService.getNotesWithTimeInInterval(this, prevRun, now);

            if (!notes.isEmpty()) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found " + notes.size() + " notes between " + prevRun + " and " + now);
                showNotification(this, notes);
            } else {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "No notes found between " + prevRun + " and " + now);
            }
        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "No previous run");
        }

        scheduleNextJob(now, now);
    }

    private long getJobRunTime(int jobId) {
        JobRequest jobRequest = JobManager.instance().getJobRequest(jobId);
        return jobRequest.getScheduledAt() + jobRequest.getStartMs();
    }

    public static List<NoteWithTime> getNotesWithTimeInInterval(
            Context context, ReadableInstant fromTime, ReadableInstant beforeTime) {

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        List<NoteWithTime> result = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Times.ContentUri.times(fromTime.getMillis()),
                null,
                null,
                null,
                null);

        if (cursor != null) {
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    long noteId = cursor.getLong(ProviderContract.Times.ColumnIndex.NOTE_ID);
                    long bookId = cursor.getLong(ProviderContract.Times.ColumnIndex.BOOK_ID);
                    String bookName = cursor.getString(ProviderContract.Times.ColumnIndex.BOOK_NAME);
                    String noteState = cursor.getString(ProviderContract.Times.ColumnIndex.NOTE_STATE);
                    String noteTitle = cursor.getString(ProviderContract.Times.ColumnIndex.NOTE_TITLE);
                    String orgTimestampString = cursor.getString(ProviderContract.Times.ColumnIndex.ORG_TIMESTAMP_STRING);

                    OrgDateTime orgDateTime = OrgDateTime.parse(orgTimestampString);

                    /* Skip if it's done-type state. */
                    if (noteState == null || !AppPreferences.doneKeywordsSet(context).contains(noteState)) {
                        List<DateTime> times = OrgDateTimeUtils.getTimesInInterval(
                                orgDateTime, fromTime, beforeTime, false, 1);

                        for (DateTime time : times) {
                            if (!orgDateTime.hasTime()) {
                                time = time.plusHours(9); // TODO: Move to preferences
                            }

                            result.add(new NoteWithTime(noteId, bookId, bookName, noteTitle, time, orgDateTime));
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Fetched times, now sorting " + result.size() + " entries by time...");

        /* Sort by time, older first. */
        Collections.sort(result, new Comparator<NoteWithTime>() {
            @Override
            public int compare(NoteWithTime o1, NoteWithTime o2) {
                if (o1.time == o2.time) {
                    return 0;
                } else if (o1.time.isBefore(o2.time)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Times sorted, total " + result.size());

        return result;
    }

    static class NoteWithTime {
        public long id;
        public long bookId;
        public String bookName;
        public String title;
        public DateTime time;
        OrgDateTime orgDateTime;

        NoteWithTime(long id, long bookId, String bookName, String title, DateTime time, OrgDateTime orgDateTime) {
            this.id = id;
            this.bookId = bookId;
            this.bookName = bookName;
            this.title = title;
            this.time = time;
            this.orgDateTime = orgDateTime;
        }
    }

    private void showNotification(Context context, List<NoteWithTime> notes) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context, notes);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        for (int i = 0; i < notes.size(); i++) {
            NoteWithTime note = notes.get(i);

            String notificationTag = String.valueOf(note.id);
            int notificationId = Notifications.REMINDER;

            String line = context.getString(R.string.scheduled_using_time, note.orgDateTime.toStringWithoutBrackets());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setColor(ContextCompat.getColor(context, R.color.notification))
                    .setSmallIcon(R.drawable.cic_orgzly_notification);

            /* Set vibration. */
            if (AppPreferences.remindersVibrate(context)) {
                builder.setVibrate(SCHEDULED_NOTE_VIBRATE_PATTERN);
            }

            /* Set notification sound. */
            if (AppPreferences.remindersSound(context)) {
                Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                builder.setSound(sound);
            }

            builder.setContentTitle(note.title);
            builder.setContentText(line);

            builder.setStyle(new NotificationCompat.InboxStyle()
                    .setSummaryText(note.bookName)
                    .addLine(line)
            );

            /* Open note on notification click. */
            PendingIntent openPi = ActivityUtils.mainActivityPendingIntent(context, note.bookId, note.id);
            builder.setContentIntent(openPi);

            /* Action text depending on repeater. */
            String doneActionText = note.orgDateTime.hasRepeater() ?
                    getString(R.string.complete_with_repeater, note.orgDateTime.getRepeater().toString()) :
                    getString(R.string.complete);

            builder.addAction(
                    R.drawable.ic_done_white_24dp,
                    doneActionText,
                    markNoteAsDonePendingIntent(context, note.id, notificationTag, notificationId));

            notificationManager.notify(notificationTag, notificationId, builder.build());
        }
    }

    private PendingIntent markNoteAsDonePendingIntent(
            Context context, long noteId, String notificationTag, int notificationId) {
        Intent intent = new Intent(context, ActionService.class);

        intent.setAction(AppIntent.ACTION_NOTE_MARK_AS_DONE);

        intent.putExtra(ActionService.EXTRA_NOTE_ID, noteId);

        intent.putExtra(ActionService.EXTRA_NOTIFICATION_TAG, notificationTag);
        intent.putExtra(ActionService.EXTRA_NOTIFICATION_ID, notificationId);

        return PendingIntent.getService(
                context,
                Long.valueOf(noteId).intValue(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Notify reminder service about changes that might affect scheduling of reminders. */
    public static void notifyDataChanged(Context context) {
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(ReminderService.EXTRA_EVENT, ReminderService.EVENT_DATA_CHANGED);
        context.startService(intent);
    }

    /** Notify ReminderService about the triggered job. */
    public static void notifyJobTriggered(Context context) {
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(ReminderService.EXTRA_EVENT, ReminderService.EVENT_JOB_TRIGGERED);
        context.startService(intent);
    }
}
