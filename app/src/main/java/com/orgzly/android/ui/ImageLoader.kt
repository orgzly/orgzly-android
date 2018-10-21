package com.orgzly.android.ui

import android.text.Spannable
import com.orgzly.android.ui.views.TextViewWithMarkup
import com.orgzly.android.ui.views.style.FileLinkSpan


object ImageLoader {
    @JvmStatic
    fun loadImages(textWithMarkup: TextViewWithMarkup) {
        val context = textWithMarkup.context

        // Only if AppPreferences.displayImages(context) is true
        // loadImages(textWithMarkup.text as Spannable)
    }

    private fun loadImages(text: Spannable) {
        SpanUtils.forEachSpan(text, FileLinkSpan::class.java) { span ->
            loadImage(span)
        }
    }

    private fun loadImage(span: FileLinkSpan) {
        val path = span.path

    }
}