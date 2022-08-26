package com.orgzly.android.ui.views.richtext

import android.text.Editable
import android.text.TextWatcher
import com.orgzly.BuildConfig
import com.orgzly.android.util.LogUtils
import java.util.regex.Pattern

class RichTextEditWatcher: TextWatcher {
    data class ListItem(val pattern: Pattern, val bullet: String)

    data class CurrentListItem(
        val listItem: ListItem,
        var currLineStart: Int,
        var nextItemStart: Int,
        var indent: String,
        var content: String)

    // TODO: Remove space at the end of bullet
    private val listItemTypes = arrayOf(
        ListItem(Pattern.compile("^(\\s*)-\\s+\\[[ X]](.*)"), "- [ ] "),
        ListItem(Pattern.compile("^(\\s*)-(.*)"), "- "),
        ListItem(Pattern.compile("^(\\s*)\\+\\s+\\[[ X]](.*)"), "+ [ ] "),
        ListItem(Pattern.compile("^(\\s*)\\+(.*)"), "+ ")
    )

    private var currentListItem : CurrentListItem? = null

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (BuildConfig.LOG_DEBUG) {
            LogUtils.d(
                TAG, "$count (count) characters beginning at"
                    + " $start (start) are about to be replaced by new text "
                    + "with length $after (after)")
        }

        if (after == count + 1) { // Adding one new character

            val endCharIndex = start + after - 1

            if (s.length == endCharIndex || s[endCharIndex] == '\n') { // End of string or line

                val currLineStart = s.lastIndexOf("\n", start - 1).let { prevNewLine ->
                    if (prevNewLine < 0) {
                        0
                    } else {
                        prevNewLine + 1
                    }
                }

                val line = s.substring(currLineStart, endCharIndex)

                currentListItem = searchForListItem(line, currLineStart, endCharIndex + 1)
            }
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
        if (BuildConfig.LOG_DEBUG) {
            LogUtils.d(
                TAG, "$count (count) characters beginning at "
                    + " $start (start) have just replaced old text that had length $before (before)")
        }

        currentListItem?.let {
            if (count == before + 1 && s[start + count - 1] == '\n') {
                // New line added
            } else {
                currentListItem = null
            }
        }
    }

    override fun afterTextChanged(s: Editable) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

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

    companion object {
        private val TAG = RichTextEditWatcher::class.java.name
    }
}