package com.orgzly.android.ui.views.style

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.android.ui.views.richtext.ActionableRichTextView

class DrawerSpan(val name: String, val content: CharSequence, var isFolded: Boolean) : ClickableSpan() {
    override fun onClick(view: View) {
        if (view is ActionableRichTextView) {
            view.toggleDrawer(this)
        }
    }

    override fun updateDrawState(tp: TextPaint) {
        // tp.isUnderlineText = true
        // tp.color = Color.GREEN
    }
}
