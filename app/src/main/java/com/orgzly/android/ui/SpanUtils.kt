package com.orgzly.android.ui

import android.text.Spanned

object SpanUtils {
    @JvmStatic
    fun <T>forEachSpan(text: Spanned, type: Class<T>, action: (span: T) -> Any) {
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

    @JvmStatic
    fun <T>getSpans(text: Spanned, type: Class<T>): List<T> {
        val list = mutableListOf<T>()
        var next: Int
        var i = 0
        while (i < text.length) {
            next = text.nextSpanTransition(i, text.length, type)

            text.getSpans(i, next, type).forEach { span ->
                list.add(span)
            }

            i = next
        }
        return list
    }
}