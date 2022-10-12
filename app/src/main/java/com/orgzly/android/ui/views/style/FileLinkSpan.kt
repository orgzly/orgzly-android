package com.orgzly.android.ui.views.style

import android.os.Handler
import android.view.View
import com.orgzly.android.ui.views.richtext.ActionableRichTextView

class FileLinkSpan(override val type: Int, val path: String, override val name: String?) : LinkSpan(type, path, name), Offsetting {
    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6 + path.length
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