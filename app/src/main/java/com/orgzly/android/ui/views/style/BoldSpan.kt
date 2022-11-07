package com.orgzly.android.ui.views.style

import android.graphics.Typeface
import android.text.style.StyleSpan

class BoldSpan : StyleSpan(Typeface.BOLD), Offsetting {
    override val characterOffset = 2
}