package com.orgzly.android.widgets;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.orgzly.android.AppIntent;

import java.util.concurrent.TimeUnit;

public class ListWidgetUpdateJob extends Job {
    public static final String TAG = ListWidgetUpdateJob.class.getName();

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        LocalBroadcastManager.getInstance(getContext())
                .sendBroadcast(new Intent(AppIntent.ACTION_LIST_WIDGET_UPDATE));

        return Result.SUCCESS;
    }

    public static void ensureScheduled() {
        JobManager.instance().cancelAllForTag(ListWidgetUpdateJob.TAG);

        new JobRequest.Builder(ListWidgetUpdateJob.TAG)
                .setPeriodic(TimeUnit.MINUTES.toMillis(15))
                .setPersisted(false)
                .build()
                .schedule();
    }
}
