package com.orgzly.android.ui.views.style

import android.graphics.Typeface
import android.text.style.StyleSpan

class ItalicSpan : StyleSpan(Typeface.ITALIC), Offsetting {
    override val characterOffset = 2
}