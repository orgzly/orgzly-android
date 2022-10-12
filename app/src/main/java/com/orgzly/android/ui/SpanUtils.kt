package com.orgzly.android.ui

import android.text.Spanned


object SpanUtils {
    @JvmStatic
    inline fun <T>forEachSpan(
        spanned: Spanned,
        type: Class<T>,
        start: Int = 0,
        end: Int = spanned.length,
        action: (span: T, curr: Int, next: Int) -> Unit) {

        forEachTransition(spanned, type, start, end) { spans, curr, next ->
            spans.forEach { span ->
                action(span, curr, next)
            }
        }
    }

    @JvmStatic
    inline fun <T>forEachTransition(
        spanned: Spanned,
        type: Class<T>,
        start: Int = 0,
        end: Int = spanned.length,
        action: (spans: Array<T>, curr: Int, next: Int) -> Unit) {

        var curr = start
        while (curr < end) {
            val next = spanned.nextSpanTransition(curr, end, type)

            val spans = spanned.getSpans(curr, next, type)

            action(spans, curr, next)

            curr = next
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