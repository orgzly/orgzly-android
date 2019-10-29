package com.orgzly.android;


import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

import com.evernote.android.job.JobConfig;
import com.evernote.android.job.JobManager;
import com.orgzly.android.di.AppComponent;
import com.orgzly.android.di.DaggerAppComponent;
import com.orgzly.android.di.module.ApplicationModule;
import com.orgzly.android.di.module.DatabaseModule;
import com.orgzly.android.ui.CommonActivity;
import com.orgzly.android.ui.CommonActivityLifecycleCallbacks;
import com.orgzly.android.ui.settings.SettingsFragment;

import org.jetbrains.annotations.Nullable;

import androidx.preference.PreferenceManager;

import dagger.Component;

public class App extends Application {

    /**
     * Job IDs for {@link androidx.core.app.JobIntentService#enqueueWork}.
     */
    public static final int ACTION_SERVICE_JOB_ID = 1;
    public static final int REMINDER_SERVICE_JOB_ID = 2;
    public static final int NOTIFICATION_SERVICE_JOB_ID = 3;
    public static final int LAST_JOB_ID = 100;

    private static Context context;

    private static CommonActivity currentActivity;

    // TODO: Inject
    public static AppExecutors EXECUTORS = new AppExecutors();

    public static AppComponent appComponent;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent
                .builder()
                .applicationModule(new ApplicationModule(this))
                .databaseModule(new DatabaseModule(false))
                .build();

//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectAll()
//                .penaltyLog()
//                .build());

        // Register CommonActivity life cycle within the App
        // to be able to access the activity in classes like OrgFormatter
        registerActivityLifecycleCallbacks(new CommonActivityLifecycleCallbacks());

        App.setDefaultPreferences(this, false);

        JobConfig.setJobIdOffset(LAST_JOB_ID);
        App.context = getApplicationContext();
        JobManager.create(this).addJobCreator(new AppJobCreator());

        NotificationChannels.createAll(this);
    }

    public static void setDefaultPreferences(Context context, boolean readAgain) {
        if (readAgain || !PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            for (int res: SettingsFragment.PREFS_RESOURCES.values()) {
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

    @Nullable
    public static CommonActivity getCurrentActivity() {
        return currentActivity;
    }
}
