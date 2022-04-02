package com.orgzly.android.ui

import android.text.Spanned

object SpanUtils {
    @JvmStatic
    fun <T>forEachSpan(spanned: Spanned, type: Class<T>, action: (span: T, start: Int, end: Int) -> Any) {
        var i = 0
        while (i < spanned.length) {
            val next = spanned.nextSpanTransition(i, spanned.length, type)

            spanned.getSpans(i, next, type).forEach { span ->
                action(span, i, next)
            }

            i = next
        }
    }

    @JvmStatic
    fun <T>getSpans(text: Spanned, type: Class<T>): List<T> {
        return mutableListOf<T>().apply {
            forEachSpan(text, type) { span, _, _ ->
                add(span)
            }
        }
    }
}