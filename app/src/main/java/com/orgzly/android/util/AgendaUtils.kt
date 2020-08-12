package com.orgzly.android.util

import com.orgzly.android.ui.notes.query.agenda.AgendaItems.ExpandableOrgRange
import com.orgzly.org.datetime.*
import org.joda.time.DateTime
import java.util.*

object AgendaUtils {
    data class ExpandedOrgRange(val isOverdueToday: Boolean, val expanded: Set<DateTime>)

    fun expandOrgDateTime(expandable: ExpandableOrgRange, now: DateTime, days: Int): ExpandedOrgRange {
        val today = now.withTimeAtStartOfDay()

        // Only unique values
        val result: MutableSet<DateTime> = LinkedHashSet()

        var rangeStart = expandable.range.startTime
        val rangeEnd = expandable.range.endTime

        // Check if overdue
        var isOverdueToday = false
        if (expandable.canBeOverdueToday) {
            val nowCal = today.toGregorianCalendar()

            if (rangeStart.calendar.before(nowCal)) {
                isOverdueToday = true
            }
        }

        var to = today.plusDays(days).withTimeAtStartOfDay()

        if (rangeEnd == null) {
            result.addAll(OrgDateTimeUtils.getTimesInInterval(
                    rangeStart, today, to, 0, true, expandable.warningPeriod, 0))

        } else { // a time range
            if (to.isAfter(rangeEnd.calendar.timeInMillis)) {
                to = DateTime(rangeEnd.calendar).withTimeAtStartOfDay().plusDays(1)
            }

            // If start time has no repeater, use a daily repeater
            if (!rangeStart.hasRepeater()) {
                val start = DateTime(rangeStart.calendar)
                val repeater = OrgRepeater(OrgRepeater.Type.CATCH_UP, 1, OrgInterval.Unit.DAY)

                rangeStart = buildOrgDateTimeFromDate(start, repeater)
            }

            result.addAll(OrgDateTimeUtils.getTimesInInterval(
                    rangeStart, today, to, 0, true, expandable.warningPeriod, 0))
        }

        return ExpandedOrgRange(isOverdueToday, TreeSet(result))
    }

    private fun buildOrgDateTimeFromDate(date: DateTime, repeater: OrgRepeater?): OrgDateTime {
        return OrgDateTime.Builder().apply {
            setYear(date.year)
            setMonth(date.monthOfYear - 1)
            setDay(date.dayOfMonth)

            setHour(date.hourOfDay)
            setMinute(date.minuteOfHour)

            if (repeater != null) {
                setRepeater(repeater)
            }
        }.build()
    }
}