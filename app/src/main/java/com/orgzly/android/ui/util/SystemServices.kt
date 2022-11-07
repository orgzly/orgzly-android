@file:JvmName("SystemServices")

package com.orgzly.android.ui.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.storage.StorageManager
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager

fun Context.getNotificationManager(): NotificationManager {
    return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

fun Context.getClipboardManager(): ClipboardManager {
    return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

fun Context.getAlarmManager(): AlarmManager {
    return getSystemService(Context.ALARM_SERVICE) as AlarmManager
}

fun Context.getConnectivityManager(): ConnectivityManager {
    return getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
}

fun Context.getLayoutInflater(): LayoutInflater {
    return getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
}

fun Context.getStorageManager(): StorageManager {
    return getSystemService(Context.STORAGE_SERVICE) as StorageManager
}

fun Context.getInputMethodManager(): InputMethodManager {
    return getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}