package com.orgzly.android.ui.views.style

import android.view.View

class SearchLinkSpan(override val type: Int, val value: String, override val name: String?) : LinkSpan(type, value, name) {
    override fun onClick(widget: View) {
    }
}