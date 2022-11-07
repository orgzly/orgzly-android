package com.orgzly.android.ui.views.style

import android.os.Handler
import android.util.Log
import android.view.View
import com.orgzly.android.ui.views.richtext.ActionableRichTextView

class FileOrNotLinkSpan(val type: Int, val link: String, val name: String?) : LinkSpan(), Offsetting {
    private val path = link

    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6 + link.length
        else -> 0
    }

    override fun onClick(view: View) {
        if (view is ActionableRichTextView) {
            Handler().post { // Run after onClick to prevent Snackbar from closing immediately
                view.followLinkToFile(path)
            }
        }
    }
}
