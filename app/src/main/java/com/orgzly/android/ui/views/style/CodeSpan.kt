package com.orgzly.android.ui.views.style

import android.text.style.TypefaceSpan

class CodeSpan : TypefaceSpan("monospace"), Offsetting {
    override val characterOffset = 2
}