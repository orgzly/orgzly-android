package com.orgzly.android.ui.views.style

import android.text.TextPaint
import android.text.style.CharacterStyle

class DrawerSpan(val name: String, val content: CharSequence, var isFolded: Boolean) :
    CharacterStyle(), Offsetting {

    override val characterOffset = if (isFolded) {
        // :START:…
        -1 + 1 + content.length + 1 + 5
    } else {
        0
    }

    override fun updateDrawState(tp: TextPaint) {
        // tp.isUnderlineText = true
        // tp.color = Color.GREEN
    }
}