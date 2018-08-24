package com.orgzly.android.ui.views

import android.content.Context
import androidx.appcompat.widget.AppCompatEditText
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils
import java.util.regex.Pattern

class EditTextWithMarkup : AppCompatEditText {
    constructor(context: Context) : super(context) {
        addTextChangedListener(textWatcher)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        addTextChangedListener(textWatcher)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        addTextChangedListener(textWatcher)
    }

    private val textWatcher: TextWatcher = object: TextWatcher {
        private var nextCheckboxPosition = -1

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, s, "Start", start, "Count", count, "After", after)

            if (s.length == start || s[start] == '\n') { // End of string or line
                var startOfLine = s.toString().lastIndexOf("\n", start - 1)
                if (startOfLine < 0) {
                    startOfLine = 0
                } else {
                    startOfLine++
                }
                val line = s.toString().substring(startOfLine, start)
                val p = Pattern.compile("(^\\s*-\\s+\\[)[ X]\\]")
                val m = p.matcher(line)
                if (m.find()) {
                    nextCheckboxPosition = start + 1
                }
            }
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, s, "Start", start, "Before", before, "Count", count)

            if (nextCheckboxPosition != -1 && before == 0 && count == 1 && start < s.length && s[start] == '\n') {
                // All set
            } else {
                nextCheckboxPosition = -1
            }
        }

        override fun afterTextChanged(s: Editable) {
            if (nextCheckboxPosition != -1) {
                s.replace(nextCheckboxPosition, nextCheckboxPosition, "- [ ] ")
            }

            nextCheckboxPosition = -1
        }
    }

    companion object {
        private val TAG = EditTextWithMarkup::class.java.name
    }
}