package com.orgzly.android.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class EditTextWithMarkup : AppCompatEditText {
    constructor(context: Context) : super(context) {
        addTextChangedListener(EditTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        addTextChangedListener(EditTextWatcher())
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        addTextChangedListener(EditTextWatcher())
    }
}