package com.orgzly.android.util

import com.orgzly.android.ui.TimeType
import com.orgzly.android.ui.notes.query.agenda.AgendaItems
import com.orgzly.org.datetime.OrgRange
import org.hamcrest.Matchers
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.*

@RunWith(value = Parameterized::class)
class AgendaUtilsTest(private val param: Parameter) {

    data class Parameter(
            val timeType: TimeType,
            val rangeStr: String,
            val days: Int,
            val dates: List<DateTime>)

    // 2017 May 5, 13:00:00
    private val now = GregorianCalendar(2017, Calendar.MAY, 5, 13, 0)

    @Test
    fun testExpander() {
        val range = OrgRange.parse(param.rangeStr)

        val expandable = AgendaItems.ExpandableOrgRange.fromRange(param.timeType, range)

        val expandedDates = AgendaUtils.expandOrgDateTime(expandable, DateTime(now), param.days)

        Assert.assertEquals(param.dates.size.toLong(), expandedDates.size.toLong())
        Assert.assertThat(
                toStringArray(expandedDates.toList()),
                Matchers.`is`(toStringArray(param.dates)))
    }

    private fun toStringArray(times: List<DateTime>): List<String>? {
        return mutableListOf<String>().apply {
            for (time in times) {
                add(time.toString())
            }
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters
        fun data(): Collection<Parameter> {
            return listOf(
                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-03 Wed>--<2017-05-11 Do>",
                            days = 2,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0),
                                    DateTime(2017, 5, 6, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-03 Wed>--<2017-05-11 Do>",
                            days = 1,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-06 Sat>--<2017-05-08 Mon>",
                            days = 10,
                            dates = listOf(
                                    DateTime(2017, 5, 6, 0, 0),
                                    DateTime(2017, 5, 7, 0, 0),
                                    DateTime(2017, 5, 8, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-02 Tue ++3d>",
                            days = 5,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0),
                                    DateTime(2017, 5, 8, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-04 Do>",
                            days = 5,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-05 Do>",
                            days = 5,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-06 Do>",
                            days = 5,
                            dates = listOf(
                                    DateTime(2017, 5, 6, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-03 Wed 09:00 ++12h>",
                            days = 2,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0), // overdue
                                    DateTime(2017, 5, 5, 21, 0),
                                    DateTime(2017, 5, 6, 9, 0),
                                    DateTime(2017, 5, 6, 21, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-05 Fri 09:00 ++12h>",
                            days = 2,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0), // overdue
                                    DateTime(2017, 5, 5, 21, 0),
                                    DateTime(2017, 5, 6, 9, 0),
                                    DateTime(2017, 5, 6, 21, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-07 Sun 09:00 ++6h>",
                            days = 4,
                            dates = listOf(
                                    DateTime(2017, 5, 7, 9, 0),
                                    DateTime(2017, 5, 7, 15, 0),
                                    DateTime(2017, 5, 7, 21, 0),
                                    DateTime(2017, 5, 8, 3, 0),
                                    DateTime(2017, 5, 8, 9, 0),
                                    DateTime(2017, 5, 8, 15, 0),
                                    DateTime(2017, 5, 8, 21, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-03 Wed 09:00 .+12h>",
                            days = 2,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0),  // overdue
                                    DateTime(2017, 5, 5, 21, 0),
                                    DateTime(2017, 5, 6, 9, 0),
                                    DateTime(2017, 5, 6, 21, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-03 Wed 09:00 +12h>",
                            days = 3,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0),  // overdue
                                    DateTime(2017, 5, 5, 21, 0),
                                    DateTime(2017, 5, 6, 9, 0),
                                    DateTime(2017, 5, 6, 21, 0),
                                    DateTime(2017, 5, 7, 9, 0),
                                    DateTime(2017, 5, 7, 21, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-08 Mon 09:00 +12h>",
                            days = 5,
                            dates = listOf(
                                    DateTime(2017, 5, 8, 9, 0),
                                    DateTime(2017, 5, 8, 21, 0),
                                    DateTime(2017, 5, 9, 9, 0),
                                    DateTime(2017, 5, 9, 21, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-06 Sat +1w>",
                            days = 5,
                            dates = listOf(
                                    DateTime(2017, 5, 6, 0, 0))),

                    Parameter(
                            timeType = TimeType.SCHEDULED,
                            rangeStr = "<2017-05-06 Sat +1w>",
                            days = 10,
                            dates = listOf(
                                    DateTime(2017, 5, 6, 0, 0),
                                    DateTime(2017, 5, 13, 0, 0))),

                    Parameter(
                            timeType = TimeType.EVENT,
                            rangeStr = "<2017-05-04 Sat +1d>",
                            days = 3,
                            dates = listOf(
                                    DateTime(2017, 5, 5, 0, 0),
                                    DateTime(2017, 5, 6, 0, 0),
                                    DateTime(2017, 5, 7, 0, 0))),

                    Parameter(
                            timeType = TimeType.DEADLINE,
                            rangeStr = "<2017-05-10 -4d>",
                            days = 3,
                            dates = listOf(
                                    DateTime(2017, 5, 6, 0, 0))) // Warning
            )
        }
    }
}