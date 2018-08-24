package com.orgzly.android.ui;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.orgzly.android.App;

// Monitor the lifecycle of CommonActivity
// This allows to access the activity in files like OrgFormatter
// and enables permission and snackbar access
public class CommonActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

    private void registerActivity(Activity activity) {
        if (activity instanceof CommonActivity) {
            App.setCurrentActivity((CommonActivity) activity);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        registerActivity(null);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        registerActivity(null);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        registerActivity(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        registerActivity(null);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        registerActivity(null);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        registerActivity(null);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        registerActivity(null);
    }
}
