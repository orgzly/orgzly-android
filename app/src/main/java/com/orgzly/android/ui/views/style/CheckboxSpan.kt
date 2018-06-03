package com.orgzly.android.ui.views.style

import android.content.Context
import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.TypefaceSpan
import android.view.View
import com.orgzly.android.ui.views.TextViewWithMarkup

class CheckboxSpan(val content: CharSequence, val rawStart: Int, val rawEnd: Int) : ClickableSpan() {

    private val typeface = TypefaceSpan("monospace")

    override fun onClick(widget: View) {
        if (widget is TextViewWithMarkup) {
            widget.toggleCheckbox(this)
        }
    }

    override fun updateDrawState(tp: TextPaint) {
        /*if(isChecked())
            tp.color = Color.GREEN
        else
            tp.color = Color.RED*/
        typeface.updateDrawState(tp)
        tp.isUnderlineText = true
    }

    fun isChecked(): Boolean {
        return content[1] != ' '
    }

}
