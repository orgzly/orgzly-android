package com.orgzly.android.ui.views.style

import android.text.style.URLSpan
import android.view.View

class UrlLinkSpan(override val type: Int, val url: String, override val name: String?) : LinkSpan(type, url, name), Offsetting {
    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6 + url.length
        else -> 0
    }

    override fun onClick(widget: View) {
        URLSpan(url).onClick(widget)
    }
}