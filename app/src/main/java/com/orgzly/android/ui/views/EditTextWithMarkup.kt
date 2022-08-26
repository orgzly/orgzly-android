package com.orgzly.android.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.GestureDetectorCompat
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

class EditTextWithMarkup : AppCompatEditText {

    data class Listeners(var onImeBack: (() -> Unit)?)

    private val listeners: Listeners by lazy { Listeners(null) }

    private var onFocusOrClickListener: OnClickListener? = null

    fun setOnFocusOrClickListener(l: OnClickListener?) {
        onFocusOrClickListener = l
    }

    fun setOnImeBackListener(listener: (() -> Unit)?) {
        listeners.onImeBack = listener
    }

    constructor(context: Context) : super(context) {
        addTextChangedListener(EditTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        addTextChangedListener(EditTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        addTextChangedListener(EditTextWatcher())
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, keyCode, event)

        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_UP) {
            listeners.onImeBack?.invoke()
            return false
        }

        return super.onKeyPreIme(keyCode, event)
    }

    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return true
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val isSingleTapUp = gestureDetector.onTouchEvent(event)

        if (isSingleTapUp) {
            onFocusOrClickListener?.onClick(this)
        }

        return super.onTouchEvent(event)
    }

    companion object {
        val TAG = EditTextWithMarkup::class.java.name
    }
}