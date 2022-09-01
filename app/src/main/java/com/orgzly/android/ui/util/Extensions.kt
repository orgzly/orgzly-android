package com.orgzly.android.ui.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.res.TypedArray
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StyleableRes
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.orgzly.android.sync.SyncRunner


fun <R> Context.styledAttributes(@StyleableRes attrs: IntArray, f: (typedArray: TypedArray) -> R): R {
    val typedArray = obtainStyledAttributes(attrs)
    try {
        return f(typedArray)
    } finally {
        typedArray.recycle()
    }
}

fun <R> Context.styledAttributes(set: AttributeSet, @StyleableRes attrs: IntArray, f: (typedArray: TypedArray) -> R): R {
    val typedArray = obtainStyledAttributes(set, attrs)
    try {
        return f(typedArray)
    } finally {
        typedArray.recycle()
    }
}


/**
 * Determines if there is internet connection available.
 */
fun Context.haveNetworkConnection(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    return if (cm != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            haveNetworkConnection(cm)

        } else {
            haveNetworkConnectionPreM(cm)
        }
    } else false

}

@TargetApi(Build.VERSION_CODES.M)
private fun haveNetworkConnection(cm: ConnectivityManager): Boolean {
    val network = cm.activeNetwork

    val capabilities = cm.getNetworkCapabilities(network)

    return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Suppress("DEPRECATION")
private fun haveNetworkConnectionPreM(cm: ConnectivityManager): Boolean {
    val networkInfo = cm.activeNetworkInfo

    if (networkInfo != null) {
        val type = networkInfo.type

        return type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_MOBILE
    }

    return false
}

fun Context.getNotificationManager(): NotificationManager {
    return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}

fun Context.getClipboardManager(): ClipboardManager {
    return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

@SuppressLint("ResourceType")
fun SwipeRefreshLayout.setup() {
    setOnRefreshListener {
        SyncRunner.startSync()
        isRefreshing = false
    }

//    context?.styledAttributes(intArrayOf(R.attr.colorPrimary, R.attr.colorOnPrimary)) { typedArray ->
//        setProgressBackgroundColorSchemeColor(typedArray.getColor(0, 0))
//        setColorSchemeColors(typedArray.getColor(1, 0))
//    }
}

fun View.removeBackgroundKeepPadding() {
    val paddingBottom = this.paddingBottom
    val paddingStart = ViewCompat.getPaddingStart(this)
    val paddingEnd = ViewCompat.getPaddingEnd(this)
    val paddingTop = this.paddingTop

    ViewCompat.setBackground(this, null)

    ViewCompat.setPaddingRelative(this, paddingStart, paddingTop, paddingEnd, paddingBottom)
}

fun View.goneIf(condition: Boolean) {
    visibility = if (condition) View.GONE else View.VISIBLE
}

fun View.goneUnless(condition: Boolean) = goneIf(!condition)

fun View.invisibleIf(condition: Boolean) {
    visibility = if (condition) View.INVISIBLE else View.VISIBLE
}

fun View.invisibleUnless(condition: Boolean) = invisibleIf(!condition)
