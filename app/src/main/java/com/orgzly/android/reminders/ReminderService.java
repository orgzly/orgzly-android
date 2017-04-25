package com.orgzly.android.reminders;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.orgzly.BuildConfig;
import com.orgzly.R;
import com.orgzly.android.Notifications;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.ui.MainActivity;
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

    public ReminderService() {
        super(TAG);
    }

    /**
     * Receives events
     *
     * @param intent
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        /* Time to use for deciding if reminders should be scheduled or not. */
        DateTime now = new DateTime();

        /* Previous run time. */
        DateTime prevRun = null;
        long prevRunMillis = AppPreferences.reminderServiceLastRun(this);
        if (prevRunMillis > 0) {
            prevRun = new DateTime(prevRunMillis);
        }

        /* Currently scheduled Job, if any. */
        int jobId = AppPreferences.reminderServiceJobId(this);

        int event = intent.getIntExtra(EXTRA_EVENT, EVENT_UNKNOWN);


        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Event: " + event);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, " Prev: " + prevRun);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "  Now: " + now);

        switch (event) {
            case EVENT_DATA_CHANGED:
                onDataChanged(now, prevRun, jobId);
                break;

            case EVENT_JOB_TRIGGERED:
                onJobTriggered(now, prevRun, jobId);
                break;

            default:
                Log.e(TAG, "Unknown event received, ignoring it");
                return;
        }

        AppPreferences.reminderServiceLastRun(this, now.getMillis());
    }

    /**
     *     prev    scheduled
     *     run        job        now
     *      |          |          |
     *  ------------------------------->
     *
     * Schedule job to run for the first time after min(now, currently scheduled job).
     */
    private void onDataChanged(DateTime now, DateTime prevRun, int prevJobId) {
        /* Cancel all jobs. */
        JobManager.instance().cancelAllForTag(ReminderJob.TAG);

        /* Schedule first time between previous run and now. */

        DateTime fromTime = prevRun;
        if (prevRun == null) {
            fromTime = now;
        }

        List<NoteWithTime> notes = ReminderService.getNotesWithTimes(this, fromTime, null);

        if (! notes.isEmpty()) {
            /* Schedule only the first upcoming time. */
            NoteWithTime firstNote = notes.get(0);

            /* Schedule *in* exactMs. */
            long exactMs = firstNote.time.getMillis() - now.getMillis();
            if (exactMs < 0) {
                exactMs = 1;
            }

            int jobId = ReminderJob.scheduleJob(exactMs);

            announceScheduledJob(jobId, firstNote);

            AppPreferences.reminderServiceJobId(this, jobId);

        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "No notes found after " + fromTime);
        }
    }

    private void announceScheduledJob(final int jobId, final NoteWithTime firstNote) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG,
                "Scheduled for " + jobRunTimeString(jobId) + " (" + firstNote.title + ")");

    }

    private String jobRunTimeString(int jobId) {
        long jobTime = getJobRunTime(jobId);
        DateTime dateTime = new DateTime(jobTime);
        return dateTime.toString();
    }

    /**
     * Display reminders for all notes with times between
     * scheduled job and now.
     *
     *     prev    scheduled
     *     run        job        now
     *      |          |          |
     *  ------------------------------->
     *                 |----------|
     *
     * Schedule next job to run for the first time after now.
     */
    private void onJobTriggered(DateTime now, DateTime prevRun, int prevJobId) {
        /* Make sure the id of triggered job is still stored (i.e. active).
         * It's possible that we tried to remove it after note update,
         * but it already got triggered. If it doesn't exist anymore, ignore it.
         */

        /* Cancel all jobs. */
        JobManager.instance().cancelAllForTag(ReminderJob.TAG);

        /* Create notifications for all notes from previous run until now. */
        List<NoteWithTime> notes = ReminderService.getNotesWithTimes(this, prevRun, now);

        if (! notes.isEmpty()) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found " + notes.size() + " notes between " + prevRun + " and " + now);
            showNotification(this, notes);
        } else {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "No notes found between " + prevRun + " and " + now);
        }
    }

    private long getJobRunTime(int jobId) {
        JobRequest jobRequest = JobManager.instance().getJobRequest(jobId);
        return jobRequest.getScheduledAt() + jobRequest.getStartMs();
    }

    public static List<NoteWithTime> getNotesWithTimes(
            Context context, ReadableInstant fromTime, ReadableInstant beforeTime) {

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
                    String noteState = cursor.getString(ProviderContract.Times.ColumnIndex.NOTE_STATE);
                    String noteTitle = cursor.getString(ProviderContract.Times.ColumnIndex.NOTE_TITLE);
                    String orgTimestampString = cursor.getString(ProviderContract.Times.ColumnIndex.ORG_TIMESTAMP_STRING);

                    OrgDateTime orgDateTime = OrgDateTime.getInstance(orgTimestampString);

                    /* Skip if it's done-type state. */
                    if (noteState == null || !AppPreferences.doneKeywordsSet(context).contains(noteState)) {
                        List<DateTime> times = OrgDateTimeUtils.getAllInstantsInInterval(
                                orgDateTime, fromTime, beforeTime, 1);

                        for (DateTime time: times) {
                            if (! orgDateTime.hasTime()) {
                                time = time.plusHours(9); // TODO: Move to preferences
                            }

                            result.add(new NoteWithTime(noteId, noteTitle, time));
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }

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

        return result;
    }

    public static class NoteWithTime {
        public long id;
        public String title;
        public DateTime time;

        public NoteWithTime(long id, String title, DateTime time) {
            this.id = id;
            this.title = title;
            this.time = time;
        }
    }

    public static void showNotification(Context context, List<NoteWithTime> notes) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context, notes);

        // TODO: Open relevant notes, agenda or search results
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setAction(Intent.ACTION_MAIN);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.cic_orgzly_notification)
                .setContentIntent(resultPendingIntent);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();

        /*
         * Reminder will be formatted differently depending on the number of notes.
         */
        if (notes.size() == 1) { // Single note
            NoteWithTime note = notes.get(0);

            style.setBigContentTitle(note.title);

            // Used if Style is not supported (API < 16)
            builder.setContentTitle(note.title);
            // No content text

        } else { // Multiple notes

            style.setBigContentTitle("You have " + notes.size() + " scheduled tasks, or something");
            String[] lines = new String[notes.size()];
            for (int i = 0; i < lines.length; i++) {
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Note: " + notes.get(i).title);
                style.addLine(notes.get(i).id + ": " + notes.get(i).title);
            }
            style.setSummaryText(context.getString(R.string.tap_to_view_notes));

            // Used if Style is not supported (API < 16)
            builder.setContentTitle("You have " + notes.size() + " scheduled tasks, or something");
            builder.setContentText(context.getString(R.string.tap_to_view_notes));
        }

        builder.setStyle(style);

        /* Reminders are important, mmmk? */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = builder.build();
        notificationManager.notify(Notifications.REMINDER_ID, notification);
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
