package com.orgzly.android.ui.notes.quickbar

import androidx.annotation.IdRes

interface QuickBarListener {
    fun onQuickBarButtonClick(@IdRes buttonId: Int, itemId: Long)
}