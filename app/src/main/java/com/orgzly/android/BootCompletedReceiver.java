package com.orgzly.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.orgzly.BuildConfig;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.widgets.ListWidgetProvider;

public class BootCompletedReceiver extends BroadcastReceiver {
    public static final String TAG = BootCompletedReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, context, intent);

        if (AppPreferences.newNoteNotification(context)) {
            Notifications.createNewNoteNotification(context);
        }

        /* restart update timer for widgets */
        ListWidgetProvider.scheduleUpdateIfNeeded(context);
    }
}
