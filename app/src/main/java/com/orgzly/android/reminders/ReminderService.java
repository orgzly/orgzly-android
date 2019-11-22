package com.orgzly.android.reminders;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.orgzly.BuildConfig;
import com.orgzly.android.App;
import com.orgzly.android.AppIntent;
import com.orgzly.android.data.DataRepository;
import com.orgzly.android.db.dao.ReminderTimeDao;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.LogUtils;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;
import com.orgzly.org.datetime.OrgInterval;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

/**
 * Every event that can affect reminders is going through this service.
 */
public class ReminderService extends JobIntentService {
    public static final String TAG = ReminderService.class.getName();

    public static final int EVENT_DATA_CHANGED = 1;
    public static final int EVENT_JOB_TRIGGERED = 2;
    public static final int EVENT_SNOOZE_JOB_TRIGGERED = 3;
    public static final int EVENT_UNKNOWN = -1;

    static final int TIME_BEFORE_NOW = 1;
    static final int TIME_FROM_NOW = 2;

    @Inject
    DataRepository dataRepository;

    public static List<NoteReminder> getNoteReminders(
            final Context context,
            DataRepository dataRepository,
            final ReadableInstant now,
            final LastRun lastRun,
            final int beforeOrAfter) {

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        final List<NoteReminder> result = new ArrayList<>();

        for (ReminderTimeDao.NoteTime noteTime: dataRepository.times()) {
            if (isRelevantNoteTime(context, noteTime)) {
                OrgDateTime orgDateTime = OrgDateTime.parse(noteTime.getOrgTimestampString());

                NoteReminderPayload payload = new NoteReminderPayload(
                        noteTime.getNoteId(),
                        noteTime.getBookId(),
                        noteTime.getBookName(),
                        noteTime.getTitle(),
                        noteTime.getTimeType(),
                        orgDateTime);

                ReadableInstant[] interval = getInterval(beforeOrAfter, now, lastRun, noteTime.getTimeType());

                DateTime time = OrgDateTimeUtils.getFirstWarningTime(
                        noteTime.getTimeType(),
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
        }

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, "Fetched times, now sorting " + result.size() + " entries by time...");

        /* Sort by time, older first. */
        Collections.sort(result, (o1, o2) -> o1.getRunTime().compareTo(o2.getRunTime()));

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Times sorted, total " + result.size());

        return result;
    }

    private static boolean isRelevantNoteTime(Context context, ReminderTimeDao.NoteTime noteTime) {
        Set<String> doneStateKeywords = AppPreferences.doneKeywordsSet(context);
        boolean isNotDone = !doneStateKeywords.contains(noteTime.getState());

        boolean isEnabled =
                AppPreferences.remindersForScheduledEnabled(context) && noteTime.getTimeType() == ReminderTimeDao.SCHEDULED_TIME
                        || AppPreferences.remindersForDeadlineEnabled(context) && noteTime.getTimeType() == ReminderTimeDao.DEADLINE_TIME
                        || AppPreferences.remindersForEventsEnabled(context) && noteTime.getTimeType() == ReminderTimeDao.EVENT_TIME;

        return isNotDone && isEnabled;
    }

    /**
     * Notify reminder service about changes that might affect scheduling of reminders.
     */
    public static void notifyDataChanged(Context context) {
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(AppIntent.EXTRA_REMINDER_EVENT, ReminderService.EVENT_DATA_CHANGED);
        enqueueWork(context, intent);
    }

    /**
     * Notify ReminderService about the triggered job.
     */
    public static void notifyJobTriggered(Context context) {
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(AppIntent.EXTRA_REMINDER_EVENT, ReminderService.EVENT_JOB_TRIGGERED);
        enqueueWork(context, intent);
    }

    public static void notifySnoozeTriggered(Context context, long noteId, int noteTimeType, long timestamp) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
        Intent intent = new Intent(context, ReminderService.class);
        intent.putExtra(AppIntent.EXTRA_REMINDER_EVENT, ReminderService.EVENT_SNOOZE_JOB_TRIGGERED);
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

                if (timeType == ReminderTimeDao.SCHEDULED_TIME) {
                    res[0] = lastRun.getScheduled();
                } else if (timeType == ReminderTimeDao.DEADLINE_TIME) {
                    res[0] = lastRun.getDeadline();
                } else {
                    res[0] = lastRun.getEvent();
                }

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
    public void onCreate() {
        App.appComponent.inject(this);

        super.onCreate();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        if (!AppPreferences.remindersForScheduledEnabled(this)
                && !AppPreferences.remindersForDeadlineEnabled(this)
                && !AppPreferences.remindersForEventsEnabled(this)) {
            showScheduledAtNotification("Reminders are disabled");
            return;
        }

        DateTime now = new DateTime();

        LastRun lastRun = LastRun.fromPreferences(this);

        int event = intent.getIntExtra(AppIntent.EXTRA_REMINDER_EVENT, EVENT_UNKNOWN);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Event: " + event);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, " Last: " + lastRun);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "  Now: " + now);

        ReminderJob.cancelAll();

        switch (event) {
            case EVENT_DATA_CHANGED:
                // Only schedule next job below
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

        scheduleNextJob(now, lastRun);

        LastRun.toPreferences(this, now);
    }

    private void scheduleNextJob(DateTime now, LastRun lastRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        List<NoteReminder> notes = ReminderService.getNoteReminders(
                this, dataRepository, now, lastRun, TIME_FROM_NOW);

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

            log = String.format(
                    Locale.getDefault(),
                    "#%d in %d at %s for %s",
                    jobId,
                    exactMs / 1000,
                    jobRunTimeString(jobId),
                    firstNote.getPayload().title);

        } else {
            log = "No notes found";
        }

        showScheduledAtNotification(log);
    }

    private String jobRunTimeString(int jobId) {
        long jobTime = getJobRunTime(jobId);
        return new DateTime(jobTime).toString();
    }

    /**
     * Display reminders for all notes with times between
     * last run and now. Then schedule the next job for times after now.
     */
    private void onJobTriggered(DateTime now, LastRun lastRun) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        String msg;

        if (lastRun != null) {
            /* Show notifications for all notes with times from previous run until now. */
            List<NoteReminder> notes = ReminderService.getNoteReminders(
                    this, dataRepository, now, lastRun, TIME_BEFORE_NOW);

            if (!notes.isEmpty()) {
                msg = "Found " + notes.size() + " notes between " + lastRun + " and " + now;
                ReminderNotifications.showNotification(this, notes);
            } else {
                msg = "No notes found between " + lastRun + " and " + now;
            }
        } else {
            msg = "No previous run";
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg);
    }

    private void onSnoozeTriggered(final Context context, final long noteId,
                                   final int noteTimeType, final long timestamp) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);

        String msg;
        // FIXME TODO replace this with a simpler query, may need to create it in notesclient
        final List<NoteReminder> result = new ArrayList<>();
        for (ReminderTimeDao.NoteTime noteTime: dataRepository.times()) {
            if (noteTime.getNoteId()== noteId && noteTime.getTimeType()== noteTimeType && isRelevantNoteTime(context, noteTime)) {
                OrgDateTime orgDateTime = OrgDateTime.parse(noteTime.getOrgTimestampString());
                NoteReminderPayload payload = new NoteReminderPayload(
                        noteTime.getNoteId(),
                        noteTime.getBookId(),
                        noteTime.getBookName(),
                        noteTime.getTitle(),
                        noteTime.getTimeType(),
                        orgDateTime);
                DateTime timestampDateTime = new DateTime(timestamp);
                result.add(new NoteReminder(timestampDateTime, payload));
            }
        }

        if (!result.isEmpty()) {
            msg = "Found " + result.size() + " notes";
            ReminderNotifications.showNotification(this, result);
        } else {
            msg = "No notes found";
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg);
    }

    private long getJobRunTime(int jobId) {
        JobRequest jobRequest = JobManager.instance().getJobRequest(jobId);
        return jobRequest.getScheduledAt() + jobRequest.getStartMs();
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

}
