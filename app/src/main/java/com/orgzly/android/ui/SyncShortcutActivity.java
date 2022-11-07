package com.orgzly.android.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.orgzly.android.sync.SyncRunner;

/**
 * Activity started by the Sync app shortcut.
 * App shortcuts require Intents that start activities, so this is used to start
 * syncing without opening the app.
 */
public class SyncShortcutActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SyncRunner.startSync();
        finish();
    }
}
