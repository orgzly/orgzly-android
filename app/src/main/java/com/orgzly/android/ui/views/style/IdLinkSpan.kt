package com.orgzly.android.ui.views.style

import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

class IdLinkSpan(val value: String) : ClickableSpan() {
    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup) {
            widget.followLinkToNoteWithProperty(PROPERTY, value)
        }
    }

    companion object {
        private const val PROPERTY = "ID"
    }
}