package com.orgzly.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orgzly.android.AppIntent.ACTION_SYNC_START
import com.orgzly.android.AppIntent.ACTION_SYNC_STOP
import com.orgzly.android.sync.SyncRunner

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, receivedIntent: Intent) {
        when (receivedIntent.action) {
            ACTION_SYNC_START -> {
                SyncRunner.startSync();
            }

            ACTION_SYNC_STOP -> {
                SyncRunner.stopSync();
            }
        }
    }
}