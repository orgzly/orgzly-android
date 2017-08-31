package com.orgzly.android;

import android.app.Application;
import android.content.Context;

import com.evernote.android.job.JobManager;

public class App extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        App.context = getApplicationContext();
        JobManager.create(this).addJobCreator(new AppJobCreator());
    }

    public static Context getAppContext() {
        return App.context;
    }
}
