package com.orgzly.android.ui.views.style

import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

class IdLinkSpan(override val type: Int, val value: String, override val name: String?) : LinkSpan(type, value, name) {
    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup) {
            widget.followLinkToNoteWithProperty(PROPERTY, value)
        }
    }

    companion object {
        private const val PROPERTY = "ID"
    }
}