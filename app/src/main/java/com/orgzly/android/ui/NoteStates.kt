package com.orgzly.android.ui

import android.content.Context

import com.orgzly.android.prefs.AppPreferences

import java.util.ArrayList

class NoteStates {
    private val values = ArrayList<String>()

    val array: Array<String>
        get() = values.toTypedArray()

    fun indexOf(keyword: String) = values.indexOf(keyword)

    operator fun get(i: Int) = values[i]


    fun getNext(keyword: String?): String? {
        if (values.isEmpty()) {
            return null
        }

        return if (keyword == null || keyword == NO_STATE_KEYWORD) {
            values.first()

        } else {
            val nextIndex = indexOf(keyword) + 1

            if (nextIndex < values.size) {
                values[nextIndex]
            } else {
                null
            }
        }
    }

    fun getPrevious(keyword: String?): String? {
        if (values.isEmpty()) {
            return null
        }

        return if (keyword == null || keyword == NO_STATE_KEYWORD) {
            values.last()

        } else {
            val nextIndex = indexOf(keyword) - 1

            if (nextIndex >= 0) {
                values[nextIndex]
            } else {
                null
            }
        }
    }

    companion object {
        const val NO_STATE_KEYWORD = "NOTE" // TODO: Stop using it

        @JvmStatic
        fun fromPreferences(context: Context): NoteStates {
            val noteStates = NoteStates()

            noteStates.values.addAll(AppPreferences.todoKeywordsSet(context))
            noteStates.values.addAll(AppPreferences.doneKeywordsSet(context))

            return noteStates
        }

        @JvmStatic
        fun isKeyword(keyword: String?): Boolean {
            return keyword != null && keyword != NO_STATE_KEYWORD
        }
    }
}