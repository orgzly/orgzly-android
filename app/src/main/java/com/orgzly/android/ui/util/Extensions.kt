package com.orgzly.android.ui.util

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.annotation.StyleableRes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.orgzly.R
import com.orgzly.android.sync.SyncService
import com.orgzly.android.ui.dialogs.TimestampDialogViewModel
import java.util.*

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
        SyncService.start(context, Intent(context, SyncService::class.java))
        isRefreshing = false
    }

    context?.styledAttributes(R.styleable.ColorScheme) { typedArray ->
        setProgressBackgroundColorSchemeColor(
                typedArray.getColor(R.styleable.ColorScheme_item_book_card_bg_color, 0))

        setColorSchemeColors(
                typedArray.getColor(R.styleable.ColorScheme_accent_color, 0))
    }
}