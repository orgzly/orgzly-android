package com.orgzly.android.ui.views

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import com.orgzly.BuildConfig
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.views.style.CheckboxSpan
import com.orgzly.android.ui.views.style.DrawerEndSpan
import com.orgzly.android.ui.views.style.DrawerSpan
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.OrgFormatter

/**
 * [TextView] with markup support.
 *
 * Used for title, content and preface text.
 */
class TextViewWithMarkup : TextViewFixed {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var onUserTextChangeListener: Runnable? = null

    private var rawText: CharSequence? = null

    fun setRawText(text: CharSequence) {
        rawText = text

        setText(OrgFormatter.parse(text, context), BufferType.SPANNABLE)
    }

    fun getRawText() : CharSequence? {
        return rawText
    }

    // TODO: Consider getting MainActivity's *VieWModel* here instead
    fun followLinkToNoteWithProperty(name: String, value: String) {
        MainActivity.followLinkToNoteWithProperty(name, value)
    }

    fun followLinkToFile(path: String) {
        MainActivity.followLinkToFile(path)
    }

    fun toggleDrawer(drawerSpan: DrawerSpan) {
        val textSpanned = text as Spanned

        val drawerStart = textSpanned.getSpanStart(drawerSpan)

        val builder = SpannableStringBuilder(text)

        if (drawerSpan.isFolded) { // Open drawer
            val replacement = drawerSpanned(drawerSpan.name, drawerSpan.content, isFolded = false)

            builder.removeSpan(drawerSpan)
            builder.replace(drawerStart, textSpanned.getSpanEnd(drawerSpan), replacement)

        } else { // Close drawer

            // Get first DrawerEndSpan after DrawerSpan
            val endSpans = textSpanned.getSpans(drawerStart, textSpanned.length, DrawerEndSpan::class.java)
            if (endSpans.isNotEmpty()) {
                val endSpan = endSpans.first()

                val replacement = drawerSpanned(drawerSpan.name, drawerSpan.content, isFolded = true)

                builder.removeSpan(drawerSpan)
                builder.removeSpan(endSpan)
                builder.replace(drawerStart, textSpanned.getSpanEnd(endSpan), replacement)

            } else {
                Log.e(TAG, "Open drawer with no DrawerEndSpan")
            }
        }

        text = builder
    }

    fun toggleCheckbox(checkboxSpan: CheckboxSpan) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, checkboxSpan)

        val textSpanned = text as Spanned

        val checkboxStart = textSpanned.getSpanStart(checkboxSpan)
        val checkboxEnd = textSpanned.getSpanEnd(checkboxSpan)

        val builder = SpannableStringBuilder(text)

        val content = if (checkboxSpan.isChecked()) "[ ]" else "[X]"

        val replacement = checkboxSpanned(content, checkboxSpan.rawStart, checkboxSpan.rawEnd)

        builder.removeSpan(checkboxSpan)
        builder.replace(checkboxStart, checkboxEnd, replacement)

        var newRawText = rawText as CharSequence
        newRawText = newRawText.replaceRange(checkboxSpan.rawStart, checkboxSpan.rawEnd, replacement)
        setRawText(newRawText)

        onUserTextChangeListener?.run()
    }

    companion object {
        fun drawerSpanned(name: String, content: CharSequence, isFolded: Boolean): Spanned {

            val begin = if (isFolded) ":$name:…" else ":$name:"
            val end = ":END:"

            val builder = SpannableStringBuilder()

            val beginSpannable = SpannableString(begin)
            beginSpannable.setSpan(
                    DrawerSpan(name, content, isFolded),
                    0,
                    beginSpannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append(beginSpannable)

            if (!isFolded) {
                val endSpannable = SpannableString(end)
                endSpannable.setSpan(
                        DrawerEndSpan(),
                        0,
                        endSpannable.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.append("\n")

                // val i = builder.length
                builder.append(content)
                // builder.setSpan(QuoteSpan(Color.GREEN), i, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

                builder.append("\n").append(endSpannable)
            }

            return builder
        }

        fun checkboxSpanned(content: CharSequence, rawStart: Int, rawEnd: Int): Spanned {

            val beginSpannable = SpannableString(content)

            beginSpannable.setSpan(
                    CheckboxSpan(content, rawStart, rawEnd),
                    0,
                    beginSpannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return beginSpannable
        }

        val TAG: String = TextViewWithMarkup::class.java.name
    }
}
