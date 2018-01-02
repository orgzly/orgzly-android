package com.orgzly.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.orgzly.R;
import com.orgzly.android.AppIntent;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncService;

/**
 * Activity started by the Sync app shortcut.
 * App shortcuts require Intents that start activities, so this is used to start
 * syncing without opening the app.
 */
public class SyncShortcutActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, SyncService.class)
                .setAction(AppIntent.ACTION_SYNC_START));

        if (!AppPreferences.showSyncNotifications(this)) {
            // Show a Toast message so the user knows something happened
            // if sync notifications are not enabled.
            Toast.makeText(this, R.string.syncing_in_progress, Toast.LENGTH_SHORT)
                    .show();
        }

        finish();
    }
}
