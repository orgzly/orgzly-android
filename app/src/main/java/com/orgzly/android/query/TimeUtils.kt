package com.orgzly.android.query

import java.util.*

object TimeUtils {
    private val FIELDS = listOf(
            Calendar.YEAR,
            Calendar.MONTH,
            Calendar.WEEK_OF_YEAR,
            Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY,
            Calendar.MINUTE,
            Calendar.SECOND,
            Calendar.MILLISECOND)

    fun timeFromNow(field: Int, amount: Int): Long {
        val before = GregorianCalendar()

        before.add(field, amount)

        // Truncate the rest
        for (index in FIELDS.indexOf(field) + 1 until FIELDS.size) {
            before.set(FIELDS[index], 0)
        }

        return before.timeInMillis
    }
}