package com.orgzly.android.reminders;

import java.lang.System;
import android.content.Context;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import com.orgzly.BuildConfig;
import com.orgzly.android.ActionService;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.prefs.AppPreferences;

public class SnoozeJob extends Job {
    public static final String TAG = SnoozeJob.class.getName();


    @Override @NonNull
    protected Result onRunJob(Params params) {
        long noteId = params.getExtras().getLong(ActionService.EXTRA_NOTE_ID, 0);
        long timestamp = params.getExtras().getLong(ActionService.EXTRA_SNOOZE_TIMESTAMP, 0);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
        ReminderService.notifySnoozeTriggered(getContext(), noteId, timestamp);
        return Result.SUCCESS;
    }

    public static void scheduleJob(Context context, long noteId, long timestamp) {
        PersistableBundleCompat extras = new PersistableBundleCompat();

        long snoozeTime = AppPreferences.remindersSnoozeTime(context) * 60 * 1000;
        long now = System.currentTimeMillis();
        long exactMs = timestamp - now;

        // keep adding snooze times until positive: handle the case where
        // someone lets the alarm go off for more that one snoozeTime interval
        while (exactMs <= 0) {
            exactMs += snoozeTime;
            timestamp += snoozeTime;
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp, exactMs);

        extras.putLong(ActionService.EXTRA_NOTE_ID, noteId);
        extras.putLong(ActionService.EXTRA_SNOOZE_TIMESTAMP, timestamp);

        new JobRequest.Builder(SnoozeJob.TAG)
            .setExact(exactMs)
            .setExtras(extras)
            .setPersisted(true)
            .build()
            .schedule();
    }
}
