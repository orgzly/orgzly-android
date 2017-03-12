package com.orgzly.android.reminders;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;

public class ReminderJob extends Job {
    public static final String TAG = ReminderJob.class.getName();

    @Override @NonNull
    protected Result onRunJob(Params params) {
        /* Notify ReminderService about the triggered job. */
        Intent intent = new Intent(getContext(), ReminderService.class);
        intent.putExtra(ReminderService.EXTRA_EVENT, ReminderService.EVENT_JOB_TRIGGERED);
        intent.putExtra(ReminderService.EXTRA_JOB_START_AT, params.getStartMs());
        getContext().startService(intent);

        return Result.SUCCESS;
    }

    public static int scheduleJob(long exactMs) {
        return new JobRequest.Builder(ReminderJob.TAG)
                .setExact(exactMs)
                .setPersisted(true)
                .build()
                .schedule();
    }
}
