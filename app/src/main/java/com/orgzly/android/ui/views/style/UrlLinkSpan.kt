package com.orgzly.android.ui.views.style

import android.text.style.URLSpan
import android.view.View

class UrlLinkSpan(override val type: Int, val url: String, override val name: String?) : LinkSpan(type, url, name) {
    override fun onClick(widget: View) {
        URLSpan(url).onClick(widget)
    }
}