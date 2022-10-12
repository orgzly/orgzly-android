package com.orgzly.android.ui.views.style

import android.view.View
import com.orgzly.android.ui.views.richtext.ActionableRichTextView

class CustomIdLinkSpan(override val type: Int, val value: String, override val name: String?) : LinkSpan(type, value, name), Offsetting {
    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6 + value.length
        else -> 0
    }

    override fun onClick(view: View) {
        if (view is ActionableRichTextView) {
            view.followLinkToNoteWithProperty(PROPERTY, value)
        }
    }

    companion object {
        private const val PROPERTY = "CUSTOM_ID"
    }
}