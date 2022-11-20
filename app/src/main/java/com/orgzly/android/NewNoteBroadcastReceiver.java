package com.orgzly.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.orgzly.BuildConfig;
import com.orgzly.android.ui.notifications.Notifications;
import com.orgzly.android.usecase.NoteCreateFromNotification;
import com.orgzly.android.usecase.UseCaseRunner;
import com.orgzly.android.util.LogUtils;

import androidx.core.app.RemoteInput;

public class NewNoteBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = NewNoteBroadcastReceiver.class.getName();

    public static final String NOTE_TITLE = "NOTE_TITLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        String title = getNoteTitle(intent);

        if (title != null) {
            App.EXECUTORS.diskIO().execute(() ->
                    UseCaseRunner.run(new NoteCreateFromNotification(title)));

            Notifications.showOngoingNotification(context);
        }
    }

    private String getNoteTitle(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        return remoteInput == null ? null : remoteInput.getString(NOTE_TITLE);
    }
}
