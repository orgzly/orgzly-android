package com.orgzly.android.ui.views.richtext

import android.content.Context
import android.graphics.Rect
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ancestors
import androidx.core.widget.NestedScrollView
import com.orgzly.BuildConfig
import com.orgzly.android.ui.util.KeyboardUtils
import com.orgzly.android.util.LogUtils

class RichTextEdit : AppCompatEditText {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val userEditingTextWatcher: TextWatcher = RichTextEditWatcher()

    fun activate(charOffset: Int) {
        visibility = View.VISIBLE

        // Position the cursor and open the keyboard
        if (charOffset in 0..(text?.length ?: 0)) {
            performClick()
            setSelection(charOffset)

            KeyboardUtils.openSoftKeyboard(this) {
                scrollForBetterCursorPosition(charOffset)
            }
        }

        addTextChangedListener(userEditingTextWatcher)
    }

    // TODO: Handle closed drawers (and such)
    private fun scrollForBetterCursorPosition(charOffset: Int) {
        val scrollView = ancestors.firstOrNull { view -> view is NestedScrollView } as? NestedScrollView

        if (scrollView != null) {
            post {
                val richText = parent as RichText

                val line = layout.getLineForOffset(charOffset)
                val baseline = layout.getLineBaseline(line)
                val ascent = layout.getLineAscent(line)

                val cursorY = richText.top + (baseline + ascent)

                val visibleHeight = Rect().let { rect ->
                    scrollView.getDrawingRect(rect)
                    rect.bottom - rect.top
                }

                val scrollTopY = scrollView.scrollY
                val scroll75pY = scrollTopY + (visibleHeight*3/4)

                // Scroll unless cursor is already in the top part of the visible rect
                val scrollTo = if (cursorY < scrollTopY) { // Too high
                    cursorY
                } else if (cursorY > scroll75pY) {  // Too low
                    cursorY - (visibleHeight*3/4)
                } else {
                    -1
                }

                if (scrollTo != -1) {
                    scrollView.smoothScrollTo(0, scrollTo)
                }

                if (BuildConfig.LOG_DEBUG) {
//                    fun pad(n: Any) = "$n".padEnd(5)
//
//                    LogUtils.d(TAG, pad(y), "this.y")
//                    LogUtils.d(TAG, pad(top), "this.top")
//                    LogUtils.d(TAG, pad(bottom), "this.bottom")
//                    LogUtils.d(TAG, pad(richText.top), "richText.top")
//                    LogUtils.d(TAG, pad(richText.bottom), "richText.bottom")
//                    LogUtils.d(TAG, pad(visibleHeight), "visibleHeight")
//                    LogUtils.d(TAG, pad(scrollTopY), "scrollTopY")
//                    LogUtils.d(TAG, pad(scroll75pY), "scroll75pY")
//                    LogUtils.d(TAG, pad(cursorY), "cursorY")
//                    Rect().let { rect -> getLocalVisibleRect(rect)
//                        LogUtils.d(TAG, pad(rect.top), "getLocalVisibleRect.top")
//                        LogUtils.d(TAG, pad(rect.bottom), "getLocalVisibleRect.bottom")
//                    }
//                    Rect().let { rect -> getWindowVisibleDisplayFrame(rect)
//                        LogUtils.d(TAG, pad(rect.top), "getWindowVisibleDisplayFrame.top")
//                        LogUtils.d(TAG, pad(rect.bottom), "getWindowVisibleDisplayFrame.bottom")
//                    }
//                    Rect().let { rect -> getGlobalVisibleRect(rect)
//                        LogUtils.d(TAG,  pad(rect.top), "getGlobalVisibleRect.top")
//                        LogUtils.d(TAG, pad(rect.bottom), "getGlobalVisibleRect.bottom")
//                    }
//                    Rect().let { rect -> scrollView.getDrawingRect(rect)
//                        LogUtils.d(TAG, pad(rect.top), "scrollView.getDrawingRect.top")
//                        LogUtils.d(TAG, pad(rect.bottom), "scrollView.getDrawingRect.bottom")
//                    }
//                    IntArray(2).let { arr -> scrollView.getLocationInWindow(arr)
//                        LogUtils.d(TAG, pad(arr[0]), "scrollView.getLocationInWindow.x")
//                        LogUtils.d(TAG, pad(arr[1]), "scrollView.getLocationInWindow.y")
//                    }
//
                    LogUtils.d(TAG, scrollTo)
                }
            }
        }
    }

    fun deactivate() {
        removeTextChangedListener(userEditingTextWatcher)

        visibility = View.GONE
    }

    /* Clear the focus on back press before letting IME handle the event. */
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, keyCode, event)

        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Clear focus before IME handling the event")
            clearFocus()
        }

        return super.onKeyPreIme(keyCode, event)
    }

//    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
//        super.onSelectionChanged(selStart, selEnd)
//        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "${selStart}-${selEnd}")
//    }

    companion object {
        val TAG: String = RichTextEdit::class.java.name
    }
}