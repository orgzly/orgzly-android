package com.orgzly.android.ui

import android.text.Spanned

object SpanUtils {
    @JvmStatic
    fun <T>forEachSpan(
        spanned: Spanned,
        type: Class<T>,
        action: (span: T, start: Int, end: Int) -> Any) {

        forEachTransition(spanned, type) { spans, start, end ->
            spans.forEach { span ->
                action(span, start, end)
            }
        }
    }

    @JvmStatic
    fun <T>forEachTransition(
        spanned: Spanned,
        type: Class<T>,
        action: (spans: Array<T>, start: Int, end: Int) -> Any) {

        var i = 0
        while (i < spanned.length) {
            val next = spanned.nextSpanTransition(i, spanned.length, type)

            val spans = spanned.getSpans(i, next, type)

            action(spans, i, next)

            i = next
        }
    }

    @JvmStatic
    fun <T>getSpans(text: Spanned, type: Class<T>): List<T> {
        return mutableListOf<T>().apply {
            forEachTransition(text, type) { spans, _, _ ->
                addAll(spans)
            }
        }
    }
}