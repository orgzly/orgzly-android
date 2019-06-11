package com.orgzly.android.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class Breadcrumbs {
    private val ssb = SpannableStringBuilder()

    fun add(name: String, truncateLength: Int = MAX_NAME_LENGTH, onClick: (() -> Unit)? = null) {
        if (ssb.isNotEmpty()) {
            ssb.append(" • ")
        }

        val start = ssb.length

        ssb.append(truncateName(name, truncateLength))

        // ssb.setSpan(StyleSpan(Typeface.BOLD), start, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (onClick != null) {
            val lastClickableSpan = object : ClickableSpan() {
                override fun updateDrawState(ds: TextPaint) {
                    // Do not color, do not underline
                }

                override fun onClick(widget: View) {
                    onClick()
                }
            }

            ssb.setSpan(lastClickableSpan, start, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun truncateName(name: String, max: Int): String {
        return if (max > 0 && name.length - 3 > max) {
            name.substring(0, max) + '…'
        } else {
            name
        }
    }

    fun toCharSequence(): CharSequence {
        return ssb
    }

    companion object {
        private const val MAX_NAME_LENGTH = 20
    }
}
