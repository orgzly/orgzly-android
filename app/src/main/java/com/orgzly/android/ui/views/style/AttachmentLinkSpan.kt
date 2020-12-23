package com.orgzly.android.ui.views.style

import android.os.Handler
import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

/**
 * This [ClickableSpan] corresponds to "attachment:" link. What comes after `:` is `path`. The full
 * path also needs a prefix which is derived from `ID` property for example.
 */
class AttachmentLinkSpan(val path: String) : ClickableSpan() {
    var prefix: String? = null

    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup && prefix != null) {
            Handler().post { // Run after onClick to prevent Snackbar from closing immediately
                widget.followLinkToFile(getPrefixedPath())
            }
        }
    }

    fun getPrefixedPath(): String {
        return "$prefix/$path"
    }
}