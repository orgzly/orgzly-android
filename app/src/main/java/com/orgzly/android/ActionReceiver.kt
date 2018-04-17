package com.orgzly.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orgzly.android.AppIntent.*
import com.orgzly.android.sync.SyncService

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, receivedIntent: Intent) {
        when (receivedIntent.action) {
            ACTION_SYNC_START, ACTION_SYNC_STOP -> {
                val intent = Intent(context, SyncService::class.java)
                intent.action = receivedIntent.action
                SyncService.start(context, intent)
            }
        }
    }
}