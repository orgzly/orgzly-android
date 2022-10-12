package com.orgzly.android.ui.views.style

import android.view.View

class SearchLinkSpan(override val type: Int, val value: String, override val name: String?) : LinkSpan(type, value, name), Offsetting {
    override val characterOffset = when (type) {
        TYPE_NO_BRACKETS -> 0
        TYPE_BRACKETS -> 4
        TYPE_BRACKETS_WITH_NAME -> 6
        else -> 0
    }

    override fun onClick(widget: View) {
    }
}