package com.orgzly.android.ui.util

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

fun SwipeRefreshLayout.setup() {
    setOnRefreshListener {
        Intent(context, SyncService::class.java).setAction(AppIntent.ACTION_SYNC_START).let {
            SyncService.start(context, it)
        }

        isRefreshing = false
    }

    context?.styledAttributes(R.styleable.ColorScheme) { typedArray ->
        setProgressBackgroundColorSchemeColor(
                typedArray.getColor(R.styleable.ColorScheme_item_book_card_bg_color, 0))

        val color = context.obtainStyledAttributes(intArrayOf(R.attr.colorAccent)).let {
            val color = it.getColor(0, 0)
            it.recycle()
            color
        }

        setColorSchemeColors(color)
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