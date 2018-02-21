package com.orgzly.android;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;

import com.evernote.android.job.JobManager;
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

    public static final AppExecutors EXECUTORS = new AppExecutors();

    @Override
    public void onCreate() {
        super.onCreate();

        App.setDefaultPreferences(this, false);

        JobManager.create(this).addJobCreator(new AppJobCreator());

        NotificationChannels.INSTANCE.createAll(this);
    }

    public static void setDefaultPreferences(Context context, boolean readAgain) {
        if (readAgain || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            for (int res: SettingsFragment.Companion.getPREFS_RESOURCES().values()) {
                PreferenceManager.setDefaultValues(context, res, true);
            }
        }
    }
}
