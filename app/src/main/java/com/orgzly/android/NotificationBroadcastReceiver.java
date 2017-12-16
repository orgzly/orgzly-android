package com.orgzly.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Pending intents from reminder notification can't call enqueueWork, so instead we make it
 * send a explicit broadcast and do that here. Required since Oreo.
 */
public class NotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationActionService.enqueueWork(context, intent);
    }
}
