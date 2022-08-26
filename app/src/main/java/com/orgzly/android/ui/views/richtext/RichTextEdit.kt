package com.orgzly.android.ui.views.richtext

import android.content.Context
import android.text.*
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import com.orgzly.BuildConfig
import com.orgzly.android.ui.util.getInputMethodManager
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
            requestFocus()

            context.getInputMethodManager()
                .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

            performClick()
            setSelection(charOffset)
        }

        addTextChangedListener(userEditingTextWatcher)
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