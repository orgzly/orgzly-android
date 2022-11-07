package com.orgzly.android.ui.views.style

import android.view.View
import com.orgzly.android.ui.views.richtext.ActionableRichTextView

class IdLinkSpan(val type: Int, val link: String, val name: String?) : LinkSpan(), Offsetting {
    private val propertyValue = link.substring(PREFIX.length)

    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6 + link.length
        else -> 0
    }

    override fun onClick(view: View) {
        if (view is ActionableRichTextView) {
            view.followLinkToNoteWithProperty(PROPERTY, propertyValue)
        }
    }

    companion object {
        private const val PROPERTY = "ID"

        const val PREFIX = "id:"
    }
}