package com.orgzly.android.ui.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import androidx.annotation.StyleableRes
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.sync.SyncService


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

@SuppressLint("ResourceType")
fun SwipeRefreshLayout.setup() {
    setOnRefreshListener {
        Intent(context, SyncService::class.java).setAction(AppIntent.ACTION_SYNC_START).let {
            SyncService.start(context, it)
        }

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
