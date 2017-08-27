package com.orgzly.android.reminders;

import java.lang.System;
import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import com.orgzly.BuildConfig;
import com.orgzly.android.ActionService;
import com.orgzly.android.util.LogUtils;

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

    public static void scheduleJob(long noteId, long timestamp) {
        PersistableBundleCompat extras = new PersistableBundleCompat();
        DateTime now = new DateTime();
        long exactMs = timestamp - now.getMillis();

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp, exactMs);

        if (exactMs > 0) {
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
}
