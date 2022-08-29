package com.orgzly.android.ui.views.style

import android.os.Handler
import android.view.View
import com.orgzly.android.ui.views.ActionableTextView

class FileLinkSpan(override val type: Int, val path: String, override val name: String?) : LinkSpan(type, path, name) {
    override fun onClick(view: View) {
        if (view is ActionableTextView) {
            Handler().post { // Run after onClick to prevent Snackbar from closing immediately
                view.followLinkToFile(path)
            }
        }
    }
}