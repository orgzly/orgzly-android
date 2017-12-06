package com.orgzly.android.query

import java.util.*

object TimeUtils {
    fun dayAfter(field: Int, amount: Int): Calendar {
        val before = GregorianCalendar()

        before.add(field, amount)

        /* Add one more day, as we use less-then operator. */
        before.add(Calendar.DAY_OF_MONTH, 1)

        /* 00:00 */
        listOf(Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND).forEach {
            before.set(it, 0)
        }

        return before
    }
}