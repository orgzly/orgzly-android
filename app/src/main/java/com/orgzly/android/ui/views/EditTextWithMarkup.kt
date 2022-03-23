package com.orgzly.android.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils

class EditTextWithMarkup : AppCompatEditText {

    data class Listeners(var onImeBack: (() -> Unit)?)

    private val listeners: Listeners by lazy { Listeners(null) }

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

    companion object {
        val TAG = EditTextWithMarkup::class.java.name
    }
}