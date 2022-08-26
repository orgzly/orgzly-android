package com.orgzly.android.ui.views.richtext

import android.graphics.Typeface
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.util.Log
import androidx.core.text.HtmlCompat
import com.orgzly.BuildConfig
import com.orgzly.android.ui.SpanUtils
import com.orgzly.android.ui.views.style.CheckboxSpan
import com.orgzly.android.ui.views.style.DrawerSpan
import com.orgzly.android.ui.views.style.LinkSpan
import com.orgzly.android.util.LogUtils

class RichTextConverter {

    private fun spannableToOrg(spanned: Spanned): String {
        return StringBuilder().also { builder ->
            spannableToOrg(builder, spanned)
        }.toString()
    }

    // TODO: Move (close to OrgFormatter)
    private fun spannableToOrg(builder: StringBuilder, spanned: Spanned) {
//        spanned.getSpans(0, spanned.length, StyleSpan::class.java).forEach {
//            Log.i(TAG, "ALL SPANS: $it")
//        }

        SpanUtils.forEachTransition(spanned, Object::class.java) { spans, curr, next ->
            Log.i(TAG, "Span transition [$curr, $next):\n    ${spans.joinToString("\n    ")}")

            spans.forEach { span ->
                when {
                    span is StyleSpan && span.style == Typeface.BOLD -> {
                        builder.append("*")
                    }

                    span is StyleSpan && span.style == Typeface.ITALIC -> {
                        builder.append("/")
                    }

                    span is LinkSpan && span.type == LinkSpan.TYPE_NO_BRACKETS ->
                        builder.append(span.link)

                    span is LinkSpan && span.type == LinkSpan.TYPE_BRACKETS ->
                        builder.append("[[").append(span.link).append("]]")

                    span is LinkSpan && span.type == LinkSpan.TYPE_BRACKETS_WITH_NAME ->
                        builder
                            .append("[[")
                            .append(span.link)
                            .append("][")
                            .append(span.name)
                            .append("]]")

                    else -> {
                    }
                }
            }

            val str = spanned.substring(curr, next)
            builder.append(str)

            for (i in spans.size - 1 downTo 0) {
                val span = spans[i]

                when {
                    span is StyleSpan && span.style == Typeface.BOLD -> {
                        builder.append("*")
                    }

                    span is StyleSpan && span.style == Typeface.ITALIC -> {
                        builder.append("/")
                    }

                    span is LinkSpan -> {
                        builder.append("]]")
                    }

                    span is LinkSpan && span.type == LinkSpan.TYPE_NO_BRACKETS ->
                        builder.append(span.link)

                    span is LinkSpan && span.type == LinkSpan.TYPE_BRACKETS ->
                        builder.append("[[").append(span.link).append("]]")

                    span is LinkSpan && span.type == LinkSpan.TYPE_BRACKETS_WITH_NAME ->
                        builder
                            .append("[[")
                            .append(span.link)
                            .append("][")
                            .append(span.name)
                            .append("]]")

                    else -> {
                    }
                }
            }
        }
    }

    private fun withinStyle(builder: StringBuilder, spanned: Spanned, curr: Int, next: Int) {
        SpanUtils.forEachTransition(spanned, LinkSpan::class.java, curr, next) { spans, start, end ->
            spans.forEach { span ->
                when (span.type) {


                }
            }



            spans.forEach { span ->
                when (span.type) {
                    LinkSpan.TYPE_NO_BRACKETS ->
                        builder.append(span.link)

                    LinkSpan.TYPE_BRACKETS ->
                        builder.append("[[").append(span.link).append("]]")

                    LinkSpan.TYPE_BRACKETS_WITH_NAME ->
                        builder
                            .append("[[")
                            .append(span.link)
                            .append("][")
                            .append(span.name)
                            .append("]]")
                }
            }
        }
    }

    private fun spannableToOrgPrev(builder: StringBuilder, spanned: Spanned) {
        spanned.getSpans(0, spanned.length, StyleSpan::class.java).forEach {
            Log.i(TAG, "ALL SPANS: $it")
        }

        SpanUtils.forEachTransition(spanned, Object::class.java) { spans, curr, next ->
            Log.i(TAG, "Span transition [$curr, $next):\n    ${spans.joinToString("\n    ")}")

            val str = spanned.substring(curr, next)

            var bold = false
            var italic = false
            var linkSpan: LinkSpan? = null

            spans.forEach { span ->
                when {
                    span is StyleSpan && span.style == Typeface.BOLD -> {
                        bold = true
                    }

                    span is StyleSpan && span.style == Typeface.ITALIC -> {
                        italic = true
                    }

                    span is LinkSpan -> {
                        linkSpan = span
                    }

                    else -> {
                    }
                }
            }

            if (bold) {
                builder.append("*")
            }

            if (italic) {
                builder.append("/")
            }

            val linkStr = linkSpan?.run {
                when (type) {
                    LinkSpan.TYPE_NO_BRACKETS        -> link
                    LinkSpan.TYPE_BRACKETS           -> "[[$link]]"
                    LinkSpan.TYPE_BRACKETS_WITH_NAME -> "[[$link][$name]]"
                    else -> null
                }
            }

            if (linkStr != null) {
                builder.append(linkStr)
            } else {
                builder.append(str)
            }

            if (italic) {
                builder.append("/")
            }

            if (bold) {
                builder.append("*")
            }
        }
    }

    private fun spannableToHtml(spanned: Spanned): String {
        return HtmlCompat.toHtml(spanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    }

    companion object {
        const val FORMAT_ORG = 1
        const val FORMAT_HTML = 2

        val TAG: String = RichTextEdit::class.java.name
    }

    class RichTextViewWatcher: TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, start, count, after)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, start, before, count)
        }

        override fun afterTextChanged(s: Editable?) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)
        }

        companion object {
            val TAG: String = RichTextViewWatcher::class.java.name
        }
    }
}