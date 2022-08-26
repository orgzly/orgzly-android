package com.orgzly.android.ui.views.style

import android.view.View
import com.orgzly.android.ui.views.richtext.ActionableRichTextView

class IdLinkSpan(override val type: Int, val value: String, override val name: String?) : LinkSpan(type, value, name) {
    override fun onClick(view: View) {
        if (view is ActionableRichTextView) {
            view.followLinkToNoteWithProperty(PROPERTY, value)
        }
    }

    companion object {
        private const val PROPERTY = "ID"
    }
}