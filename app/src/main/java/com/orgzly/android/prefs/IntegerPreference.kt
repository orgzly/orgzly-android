package com.orgzly.android.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference
import com.orgzly.R

class IntegerPreference : EditTextPreference {
    private var min = Integer.MIN_VALUE
    private var max = Integer.MAX_VALUE

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        parseAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        parseAttrs(attrs)
    }

    private fun parseAttrs(attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.IntegerRange)

            min = typedArray.getInt(R.styleable.IntegerRange_min, Integer.MIN_VALUE)
            max = typedArray.getInt(R.styleable.IntegerRange_max, Integer.MAX_VALUE)

            typedArray.recycle()
        }
    }

    override fun setText(text: String) {
        validateIntegerValue(text)?.let {
            super.setText(it)
            summary = it
        }
    }

    private fun validateIntegerValue(s: String): String? {
        if (s.isNotEmpty()) {
            try {
                val n = Integer.parseInt(s)
                if (n in min..max) {
                    return n.toString()
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }
        }

        return null
    }
}
