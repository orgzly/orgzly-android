package com.orgzly.android.ui.views.style

import android.view.View

class SearchLinkSpan(val type: Int, val link: String, val name: String?) : LinkSpan(), Offsetting {
    // private val searchString = link

    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6
        else -> 0
    }

    override fun onClick(widget: View) {
    }
}