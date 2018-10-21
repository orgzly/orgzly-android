package com.orgzly.android.ui

import android.text.Spannable

object SpanUtils {
    fun <T>forEachSpan(text: Spannable, type: Class<T>, action: (span: T) -> Any) {
        var next: Int
        var i = 0
        while (i < text.length) {
            next = text.nextSpanTransition(i, text.length, type)

            text.getSpans(i, next, type).forEach { span ->
                action(span)
            }

            i = next
        }
    }
}