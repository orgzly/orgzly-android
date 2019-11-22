package com.orgzly.android.reminders;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.orgzly.BuildConfig;
import com.orgzly.android.AppIntent;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.LogUtils;

public class SnoozeJob extends Job {
    public static final String TAG = SnoozeJob.class.getName();


    @Override @NonNull
    protected Result onRunJob(Params params) {
        long noteId = params.getExtras().getLong(AppIntent.EXTRA_NOTE_ID, 0);
        int noteTimeType = params.getExtras().getInt(AppIntent.EXTRA_NOTE_TIME_TYPE, 0);
        long timestamp = params.getExtras().getLong(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
        ReminderService.notifyForSnoozeTriggered(getContext(), noteId, noteTimeType, timestamp);
        return Result.SUCCESS;
    }

    public static void scheduleJob(Context context, long noteId, int noteTimeType, long timestamp) {
        PersistableBundleCompat extras = new PersistableBundleCompat();

        long snoozeTime = AppPreferences.remindersSnoozeTime(context) * 60 * 1000;
        String snoozeRelativeTo = AppPreferences.remindersSnoozeRelativeTo(context);
        long exactMs;
        if (snoozeRelativeTo.equals("button")) {
            exactMs = snoozeTime;
        } else if (snoozeRelativeTo.equals("alarm")) {
            timestamp += snoozeTime;
            exactMs = timestamp - System.currentTimeMillis();
            // keep adding snooze times until positive: handle the case where
            // someone lets the alarm go off for more that one snoozeTime interval
            while (exactMs <= 0) {
                exactMs += snoozeTime;
                timestamp += snoozeTime;
            }
        } else {
            // should never happen
            Log.e(TAG, "unhandled snoozeRelativeTo " + snoozeRelativeTo);
            return;
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, noteTimeType, timestamp, exactMs);

        extras.putLong(AppIntent.EXTRA_NOTE_ID, noteId);
        extras.putInt(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType);
        extras.putLong(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp);

        new JobRequest.Builder(SnoozeJob.TAG)
            .setExact(exactMs)
            .setExtras(extras)
            .build()
            .schedule();
    }
}
