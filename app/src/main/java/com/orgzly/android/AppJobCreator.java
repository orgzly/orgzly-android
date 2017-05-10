package com.orgzly.android;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.orgzly.android.reminders.ReminderJob;

public class AppJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        if (ReminderJob.TAG.equals(tag)) {
            return new ReminderJob();

        } else {
            return null;
        }
    }
}
