package com.orgzly.android.ui.views.style

import android.text.style.TypefaceSpan

class VerbatimSpan : TypefaceSpan("monospace"), Offsetting {
    override val characterOffset = 2
}