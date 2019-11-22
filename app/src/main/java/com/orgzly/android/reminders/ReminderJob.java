package com.orgzly.android.reminders;

import androidx.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;

public class ReminderJob extends Job {
    public static final String TAG = ReminderJob.class.getName();

    @Override @NonNull
    protected Result onRunJob(Params params) {
        ReminderService.notifyForJobTriggered(getContext());

        return Result.SUCCESS;
    }

    static int scheduleJob(long exactMs) {
        return new JobRequest.Builder(ReminderJob.TAG)
                .setExact(exactMs)
                .build()
                .schedule();
    }

    static void cancelAll() {
        JobManager.instance().cancelAllForTag(ReminderJob.TAG);
    }
}
