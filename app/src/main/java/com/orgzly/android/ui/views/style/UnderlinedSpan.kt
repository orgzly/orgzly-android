package com.orgzly.android.ui.views.style

import android.text.style.UnderlineSpan

class UnderlinedSpan : UnderlineSpan(), Offsetting {
    override val characterOffset = 2
}