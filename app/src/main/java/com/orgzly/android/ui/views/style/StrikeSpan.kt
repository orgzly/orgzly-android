package com.orgzly.android.ui.views.style

import android.text.style.StrikethroughSpan

class StrikeSpan : StrikethroughSpan(), Offsetting {
    override val characterOffset = 2
}