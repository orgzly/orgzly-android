package com.orgzly.android.ui.views.richtext

import android.annotation.SuppressLint
import android.content.Context
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GestureDetectorCompat
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.SpanUtils
import com.orgzly.android.ui.util.styledAttributes
import com.orgzly.android.ui.views.style.CheckboxSpan
import com.orgzly.android.ui.views.style.DrawerMarkerSpan
import com.orgzly.android.ui.views.style.Offsetting
import com.orgzly.android.util.LogUtils

class RichTextView : AppCompatTextView, ActionableRichTextView {

    fun interface OnTapUpListener {
        fun onTapUp(x: Float, y: Float, charOffset: Int)
    }

    private data class Listeners(
        var onTapUp: OnTapUpListener? = null,
        var onActionListener: ActionableRichTextView? = null)

    private val listeners = Listeners()

    fun setOnTapUpListener(listener: OnTapUpListener) {
        listeners.onTapUp = listener
    }

    fun setOnActionListener(listener: ActionableRichTextView) {
        listeners.onActionListener = listener
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

    private val hideRichTextSymbols = !AppPreferences.styledTextWithMarks(context)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        interceptClickableSpan(event)?.let { handled ->
            // if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "ClickableSpan intercepted")
            return handled
        }

        val isSingleTapUp = singleTapUpDetector.onTouchEvent(event)

        if (isSingleTapUp) {
            val tapCharOffset = getOffsetForPosition(event.x, event.y)

            val spansChars = if (hideRichTextSymbols) {
                offsettingSpansOffset(tapCharOffset)
            } else {
                0
            }

            if (BuildConfig.LOG_DEBUG && event.action != MotionEvent.ACTION_MOVE) {
                LogUtils.d(TAG, tapCharOffset, spansChars, event)
            }

            listeners.onTapUp?.onTapUp(event.x, event.y, tapCharOffset + spansChars)
        }

        return super.onTouchEvent(event)
    }

    private fun offsettingSpansOffset(tapCharOffset: Int): Int {
        var spansChars = 0

        run allDone@ {
            SpanUtils.forEachSpan(text as Spannable, Offsetting::class.java) { span, curr, next ->
                if (tapCharOffset < next) {
                    return@allDone
                }
                spansChars += span.characterOffset
                LogUtils.d(TAG, span, curr, next, tapCharOffset, span.characterOffset)
            }
        }

        return spansChars
    }


    fun activate() {
        visibility = View.VISIBLE
    }

    fun deactivate() {
        visibility = View.GONE
    }

    // Just pass to RichView
    override fun toggleDrawer(drawerSpan: DrawerMarkerSpan) {
        listeners.onActionListener?.toggleDrawer(drawerSpan)
    }

    // Just pass to RichView
    override fun toggleCheckbox(checkboxSpan: CheckboxSpan) {
        listeners.onActionListener?.toggleCheckbox(checkboxSpan)
    }

    // Just pass to RichView
    override fun followLinkToNoteWithProperty(name: String, value: String) {
        listeners.onActionListener?.followLinkToNoteWithProperty(name, value)
    }

    // Just pass to RichView
    override fun followLinkToFile(path: String) {
        listeners.onActionListener?.followLinkToFile(path)
    }

    companion object {
        val TAG: String = RichTextView::class.java.name
    }
}