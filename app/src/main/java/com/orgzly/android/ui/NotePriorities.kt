package com.orgzly.android.ui

import android.content.Context
import com.orgzly.android.prefs.AppPreferences
import java.util.*

class NotePriorities {
    private val values = ArrayList<String>()

    val array: Array<String>
        get() = values.toTypedArray()

    fun indexOf(keyword: String) = values.indexOf(keyword)

    operator fun get(i: Int) = values[i]

    companion object {
        @JvmStatic
        fun fromPreferences(context: Context): NotePriorities {
            val obj = NotePriorities()

            val lastPriority = AppPreferences.minPriority(context)

            require(lastPriority != null && lastPriority.length == 1) {
                "Last priority must be a character, not $lastPriority"
            }

            'A'.rangeTo(lastPriority[0]).forEach { p -> obj.values.add(p.toString()) }

            return obj
        }
    }
}