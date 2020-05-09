package com.orgzly.android.ui.views

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.widget.AppCompatTextView
import android.text.Layout
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat

/**
 * [TextView] with few fixes and workarounds.
 */
open class TextViewFixed : AppCompatTextView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    private var onFocusOrClickListener: OnClickListener? = null

    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return true
        }
    })

    fun setOnFocusOrClickListener(l: OnClickListener?) {
        onFocusOrClickListener = l
    }

    /**
     * Added as setMovementMethod was preventing clicks outside of ClickableSpan.
     * https://stackoverflow.com/q/30452627
     **/
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val isSingleTapUp = gestureDetector.onTouchEvent(event)

        val layout = this.layout

        if (layout != null) {
            val line = layout.getLineForVertical(event.y.toInt() - totalPaddingTop)
            val offset = getOffsetForPosition(event.x, event.y)

            if (isEventOnText(event, layout, line) && !TextUtils.isEmpty(text) && text is Spanned) {
                val spanned = text as Spanned

                val clickableSpans = spanned.getSpans(offset, offset, ClickableSpan::class.java)

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

        if (isSingleTapUp) {
            onFocusOrClickListener?.onClick(this)
        }

        return super.onTouchEvent(event)
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

    /**
     * Workaround for https://code.google.com/p/android/issues/detail?id=191430
     * (IllegalArgumentException when marking text and then clicking on the view)
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val selectionStart = selectionStart
        val selectionEnd = selectionEnd

        if (selectionStart != selectionEnd) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val text = text
                setText(null)
                setText(text)
            }
        }

        return super.dispatchTouchEvent(event)
    }
}