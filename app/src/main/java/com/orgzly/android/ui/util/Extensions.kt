package com.orgzly.android.ui.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
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
import org.joda.time.Period
import org.joda.time.format.PeriodFormat

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
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        haveNetworkConnection(getConnectivityManager())
    } else {
        haveNetworkConnectionPreM(getConnectivityManager())
    }
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

@SuppressLint("ResourceType")
fun SwipeRefreshLayout.setup() {
    setOnRefreshListener {
        SyncRunner.startSync()
        isRefreshing = false
    }
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

fun Long.userFriendlyPeriod(): String {
    return PeriodFormat.getDefault().print(Period(this))
}