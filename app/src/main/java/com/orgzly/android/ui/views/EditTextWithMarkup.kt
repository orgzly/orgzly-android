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

    data class ListItem(val pattern: Pattern, val bullet: String)

    data class CurrentListItem(
            val listItem: ListItem,
            var currLineStart: Int,
            var nextItemStart: Int,
            var indent: String,
            var content: String)

    private val textWatcher: TextWatcher = object: TextWatcher {

        // TODO: Remove space at the end of bullet
        private val listItemTypes = arrayOf(
                ListItem(Pattern.compile("^(\\s*)-\\s+\\[[ X]](.*)"), "- [ ] "),
                ListItem(Pattern.compile("^(\\s*)-(.*)"), "- "),
                ListItem(Pattern.compile("^(\\s*)\\+\\s+\\[[ X]](.*)"), "+ [ ] "),
                ListItem(Pattern.compile("^(\\s*)\\+(.*)"), "+ ")
        )

        private var currentListItem : CurrentListItem? = null

        /*
         * count: 0
         * after: 1
         *               start
         *   0   1   2     3
         * +---+---+---+
         * | - |   | ~ |
         * +---+---+---+
         *       s
         */
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Within '$s', $count characters beginning at $start"
                        + " are about to be replaced by new text with length $after")

            if (s.length == start || s[start] == '\n') { // End of string or line

                val currLineStart = s.lastIndexOf("\n", start - 1).let { prevNewLine ->
                    if (prevNewLine < 0) {
                        0
                    } else {
                        prevNewLine + 1
                    }
                }

                val line = s.substring(currLineStart, start)

                currentListItem = searchForListItem(line, currLineStart, start + 1)
            }
        }

        private fun searchForListItem(line: String, currLineStart: Int, nextItemStart: Int): CurrentListItem? {
            listItemTypes.forEach { listItemType ->
                listItemType.pattern.matcher(line).let { m ->
                    if (m.find()) {
                        return CurrentListItem(
                                listItemType,
                                currLineStart,
                                nextItemStart,
                                m.group(1)!!,
                                m.group(2)!!)
                    }
                }
            }

            return null
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Within '$s', the $count characters"
                    + " beginning at $start have just replaced old text that had length $before")

            currentListItem?.let {
                if (before == 0 && count == 1 && start < s.length && s[start] == '\n') {
                    // New line
                } else {
                    currentListItem = null
                }
            }
        }

        override fun afterTextChanged(s: Editable) {
            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Somewhere within '$s', the text has been changed")

            currentListItem?.let {
                // Remove bullet if content is empty
                // if (it.content.matches(Regex("\\s*"))) {
                    // s.replace(it.currLineStart, it.nextItemStart, "")
                // } else {
                    s.replace(it.nextItemStart, it.nextItemStart, "${it.indent}${it.listItem.bullet}")
                // }

                currentListItem = null
            }
        }
    }

    companion object {
        private val TAG = EditTextWithMarkup::class.java.name
    }
}