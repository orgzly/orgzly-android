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

    companion object {
        const val NO_STATE_KEYWORD = "NOTE"

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