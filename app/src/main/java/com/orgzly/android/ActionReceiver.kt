package com.orgzly.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orgzly.android.AppIntent.*
import com.orgzly.android.sync.SyncService

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, receivedIntent: Intent) {
        when (val action = receivedIntent.action) {
            ACTION_SYNC_START, ACTION_SYNC_STOP -> {
                SyncService.start(context, action)
            }
        }
    }
}