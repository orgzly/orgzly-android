package com.orgzly.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.orgzly.R;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.ui.ShareActivity;

import java.util.Arrays;

public class Shortcuts {
    public static void setDynamicShortcuts(Context context) {
        // App shortcuts are an Android 7 and above feature
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1)
            return;

        // Make a shortcut for creating a new note with ShareActivity
        ShortcutInfo newNoteShortcut = new ShortcutInfo.Builder(context, "new_note")
                .setShortLabel(context.getString(R.string.new_note))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_add_black_24dp))
                .setIntent(ShareActivity.createNewNoteTaskBuilder(context)
                        .getIntents()[0])
                .build();

        // Make a shortcut to start syncing using SyncService
        ShortcutInfo syncShortcut = new ShortcutInfo.Builder(context, "sync")
                .setShortLabel(context.getString(R.string.sync))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_sync_black_24dp))
                .setIntent(new Intent(context, SyncTrampolineActivity.class)
                        // Shortcuts require Intents to have actions.
                        .setAction(AppIntent.ACTION_SYNC_START))
                .build();

        // Set up shortcuts
        ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
        shortcutManager.setDynamicShortcuts(Arrays.asList(newNoteShortcut, syncShortcut));
    }

    /**
     * Activity started by the Sync shortcut.
     * App shortcuts require Intents that start activities, so this is used to start
     * syncing without opening the app.
     */
    public static class SyncTrampolineActivity extends AppCompatActivity {
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
}
