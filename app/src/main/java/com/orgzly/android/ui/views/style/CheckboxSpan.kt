package com.orgzly.android.ui.views.style

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

/**
 * @param content is `[ ]` or `[X]`
 * @param rawStart is a position of dash
 */
class CheckboxSpan(val content: CharSequence, val rawStart: Int, val rawEnd: Int) : ClickableSpan() {

    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup) {
            widget.toggleCheckbox(this)
        }
    }

    override fun updateDrawState(tp: TextPaint) {
    }

    fun isChecked(): Boolean {
        return content[1] != ' '
    }
}
