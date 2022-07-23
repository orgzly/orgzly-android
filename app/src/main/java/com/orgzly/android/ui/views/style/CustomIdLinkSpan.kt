package com.orgzly.android.ui.views.style

import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

class CustomIdLinkSpan(override val type: Int, val value: String, override val name: String?) : LinkSpan(type, value, name) {
    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup) {
            widget.followLinkToNoteWithProperty(PROPERTY, value)
        }
    }

    companion object {
        private const val PROPERTY = "CUSTOM_ID"
    }
}