package com.orgzly.android;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.orgzly.android.reminders.ReminderJob;
import com.orgzly.android.reminders.SnoozeJob;

public class AppJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        if (ReminderJob.TAG.equals(tag)) {
            return new ReminderJob();

        } else if (SnoozeJob.TAG.equals(tag)) {
            return new SnoozeJob();

        } else {
            return null;
        }
    }
}
