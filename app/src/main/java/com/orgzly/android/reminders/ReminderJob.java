package com.orgzly.android.reminders;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

public class ReminderJob extends Job {
    public static final String TAG = ReminderJob.class.getName();

    @Override @NonNull
    protected Result onRunJob(Params params) {
        ReminderService.notifyJobTriggered(getContext());

        return Result.SUCCESS;
    }

    public static int scheduleJob(long exactMs) {
        return new JobRequest.Builder(ReminderJob.TAG)
                .setExact(exactMs)
                .setPersisted(true)
                .build()
                .schedule();
    }

    public static void cancelAll() {
        JobManager.instance().cancelAllForTag(ReminderJob.TAG);
    }
}
