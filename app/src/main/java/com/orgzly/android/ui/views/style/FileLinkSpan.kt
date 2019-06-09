package com.orgzly.android.ui.views.style

import android.os.Handler
import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

class FileLinkSpan(val path: String) : ClickableSpan() {
    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup) {
            Handler().post { // Run after onClick to prevent Snackbar from closing immediately
                widget.followLinkToFile(path)
            }
        }
    }
}