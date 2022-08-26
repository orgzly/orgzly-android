package com.orgzly.android.ui.views.richtext

import android.annotation.SuppressLint
import android.content.Context
import android.text.*
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GestureDetectorCompat
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.ui.main.MainActivity
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.ui.views.style.CheckboxSpan
import com.orgzly.android.ui.views.style.DrawerEndSpan
import com.orgzly.android.ui.views.style.DrawerSpan
import com.orgzly.android.util.LogUtils

class RichTextView : AppCompatTextView, ActionableRichTextView {

    fun interface OnTapUpListener {
        fun onTapUp(x: Float, y: Float, charOffset: Int)
    }

    fun interface OnUserTextChangeListener {
        fun onUserTextChange(str: String)
    }

    private data class Listeners(
        var onTapUp: OnTapUpListener? = null,
        var onUserTextChange: OnUserTextChangeListener? = null)

    private val listeners = Listeners()

    fun setOnTapUpListener(listener: OnTapUpListener) {
        listeners.onTapUp = listener
    }

    fun setOnUserTextChangeListener(listener: OnUserTextChangeListener) {
        listeners.onUserTextChange = listener
    }


    constructor(context: Context) : super(context) {
        // addTextChangedListener(EditTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        // addTextChangedListener(EditTextWatcher())
        parseAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        // addTextChangedListener(EditTextWatcher())
        parseAttrs(attrs)
    }


    private var parseCheckboxes = true

    private fun parseAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            context.styledAttributes(attrs, R.styleable.RichText) { typedArray ->
                parseCheckboxes = typedArray.getBoolean(R.styleable.RichText_parse_checkboxes, true)
            }
        }
    }

    // TODO: Consider getting MainActivity's *ViewModel* here instead
    override fun followLinkToNoteWithProperty(name: String, value: String) {
        MainActivity.followLinkToNoteWithProperty(name, value)
    }

    override fun followLinkToFile(path: String) {
        MainActivity.followLinkToFile(path)
    }

    private val singleTapUpDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return true
        }
    })

    /**
     * Added as setMovementMethod was preventing clicks outside of ClickableSpan.
     * https://stackoverflow.com/q/30452627
     */
    private fun interceptClickableSpan(event: MotionEvent): Boolean? {
        layout?.let { layout ->
            val line = layout.getLineForVertical(event.y.toInt() - totalPaddingTop)

            if (isEventOnText(event, layout, line) && !TextUtils.isEmpty(text) && text is Spanned) {
                val spanned = text as Spanned

                val charOffset = getOffsetForPosition(event.x, event.y)
                val clickableSpans = spanned.getSpans(charOffset, charOffset, ClickableSpan::class.java)

                if (clickableSpans.isNotEmpty()) {
                    return when (event.action) {
                        MotionEvent.ACTION_DOWN ->
                            true

                        MotionEvent.ACTION_UP -> {
                            clickableSpans[0].onClick(this)
                            super.onTouchEvent(event)
                        }

                        else ->
                            super.onTouchEvent(event)
                    }
                }
            }
        }

        return null
    }

    /**
     * Check if event's coordinates are within line's text.
     *
     * Needed as getOffsetForHorizontal will return closest character,
     * which is an issue when clicking the empty space next to the text.
     */
    private fun isEventOnText(event: MotionEvent, layout: Layout, line: Int): Boolean {
        val left = layout.getLineLeft(line) + totalPaddingLeft
        val right = layout.getLineRight(line) + totalPaddingRight
        val bottom = (layout.getLineBottom(line) + totalPaddingTop).toFloat()
        val top = (layout.getLineTop(line) + totalPaddingTop).toFloat()

        return event.x in left..right && event.y in top..bottom
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (BuildConfig.LOG_DEBUG && event.action != MotionEvent.ACTION_MOVE) {
            val charOffset = getOffsetForPosition(event.x, event.y)
            LogUtils.d(TAG, charOffset, event)
        }

        interceptClickableSpan(event)?.let { handled ->
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "ClickableSpan intercepted")
            return handled
        }

        val isSingleTapUp = singleTapUpDetector.onTouchEvent(event)

        if (isSingleTapUp) {
            val charOffset = getOffsetForPosition(event.x, event.y)
            if (BuildConfig.LOG_DEBUG && event.action != MotionEvent.ACTION_MOVE) {
                LogUtils.d(TAG, charOffset, event)
            }
            listeners.onTapUp?.onTapUp(event.x, event.y, charOffset)
        }

        return super.onTouchEvent(event)
    }

    override fun toggleDrawer(drawerSpan: DrawerSpan) {
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

    override fun toggleCheckbox(checkboxSpan: CheckboxSpan) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, checkboxSpan)

        val content = if (checkboxSpan.isChecked()) "[ ]" else "[X]"
        val replacement = checkboxSpanned(content, checkboxSpan.rawStart, checkboxSpan.rawEnd)

        val newSource = text
            ?.replaceRange(checkboxSpan.rawStart, checkboxSpan.rawEnd, replacement)
            ?.toString()
            ?: ""

        listeners.onUserTextChange?.onUserTextChange(newSource)
    }

    fun activate() {
        visibility = View.VISIBLE
    }

    fun deactivate() {
        visibility = View.GONE
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

        val TAG: String = RichTextView::class.java.name
    }
}