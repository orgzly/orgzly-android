package com.orgzly.android.query

import android.util.Log
import java.util.*

object TimeUtils {
    private val TRUNCATE_FIELDS = arrayOf(
            arrayOf(Calendar.YEAR),
            arrayOf(Calendar.MONTH),
            arrayOf(Calendar.DAY_OF_MONTH, Calendar.WEEK_OF_YEAR),
            arrayOf(Calendar.HOUR_OF_DAY),
            arrayOf(Calendar.MINUTE),
            arrayOf(Calendar.SECOND),
            arrayOf(Calendar.MILLISECOND))

    private val ADD_ONE_MORE = mapOf(
            Calendar.YEAR to Calendar.DAY_OF_MONTH,
            Calendar.MONTH to Calendar.DAY_OF_MONTH,
            Calendar.DAY_OF_MONTH to Calendar.DAY_OF_MONTH,
            Calendar.WEEK_OF_YEAR to Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY to Calendar.HOUR_OF_DAY,
            Calendar.MINUTE to Calendar.MINUTE,
            Calendar.SECOND to Calendar.SECOND,
            Calendar.MILLISECOND to Calendar.MILLISECOND
    )

    fun timeFromNow(field: Int, amount: Int, addOneMore: Boolean = false): Long {
        Log.i("WIP", "timeFromNow: " + field + " " + amount)
        val before = GregorianCalendar()
        Log.i("WIP", "timeFromNow: " + before.time)
        before.add(field, amount)

        if (addOneMore) {
            ADD_ONE_MORE[field]?.let {
                before.add(it, 1)
            }
        }

        Log.i("WIP", "timeFromNow: " + before.time)

        truncate(before, field)

        Log.i("WIP", "timeFromNow: " + before.time)
        return before.timeInMillis
    }

    /** Truncate all fields after the specified one. */
    private fun truncate(time: Calendar, field: Int) {
        var startTruncating = false
        for (i in TRUNCATE_FIELDS) {
            if (startTruncating) {
                time.set(i.first(), 0)
            } else {
                for (j in i) {
                    if (j == field) {
                        startTruncating = true
                        break
                    }
                }
            }
        }
    }
}