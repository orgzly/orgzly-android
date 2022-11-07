package com.orgzly.android.ui.views.style

import android.text.style.ClickableSpan

abstract class LinkSpan : ClickableSpan() {
    companion object {
        // FIXME: These are specific to org format
        const val TYPE_NO_BRACKETS = 1
        const val TYPE_BRACKETS = 2
        const val TYPE_BRACKETS_WITH_NAME = 3
    }
}