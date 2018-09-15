package com.orgzly.android;


import android.content.Context;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.evernote.android.job.JobConfig;
import com.evernote.android.job.JobManager;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.CommonActivityLifecycleCallbacks;
import com.orgzly.android.ui.settings.SettingsFragment;

/**
 * MultiDexApplication
 */
public class App extends MultiDexApplication {
    /**
     * Job IDs for {@link android.support.v4.app.JobIntentService#enqueueWork}.
     */
    public static final int ACTION_SERVICE_JOB_ID = 1;
    public static final int REMINDER_SERVICE_JOB_ID = 2;
    public static final int NOTIFICATION_SERVICE_JOB_ID = 3;
    public static final int LAST_JOB_ID = 100;

    public static final AppExecutors EXECUTORS = new AppExecutors();

    private static Context context;
    private static CommonActivity currentActivity;

    @Override
    public void onCreate() {
        // Register CommonActivity life cycle within the App
        // to be able to access the activity in classes like OrgFormatter
        registerActivityLifecycleCallbacks(new CommonActivityLifecycleCallbacks());

        super.onCreate();

        App.setDefaultPreferences(this, false);

        JobConfig.setJobIdOffset(LAST_JOB_ID);
        App.context = getApplicationContext();
        JobManager.create(this).addJobCreator(new AppJobCreator());

        NotificationChannels.createAll(this);
    }

    public static void setDefaultPreferences(Context context, boolean readAgain) {
        if (readAgain || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            for (int res: SettingsFragment.getPREFS_RESOURCES().values()) {
                PreferenceManager.setDefaultValues(context, res, true);
            }
        }
    }

    public static Context getAppContext() {
        return App.context;
    }

    // Getter/setter for the current activity
    public static void setCurrentActivity(CommonActivity currentCommonActivity) {
        currentActivity = currentCommonActivity;
    }

    public static CommonActivity getCurrentActivity() {
        return currentActivity;
    }
}
