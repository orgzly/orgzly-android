package com.orgzly.android.util

import com.orgzly.android.ui.notes.query.agenda.AgendaItems.ExpandableOrgRange
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgDateTimeUtils
import com.orgzly.org.datetime.OrgRange
import com.orgzly.org.datetime.OrgRepeater
import org.joda.time.DateTime
import java.util.*

object AgendaUtils {
    fun expandOrgDateTime(
            expandableRanges: Array<ExpandableOrgRange>,
            now: DateTime,
            days: Int
    ): Set<DateTime> {

        val set: MutableSet<DateTime> = TreeSet()

        for (expandableRange in expandableRanges) {
            OrgRange.parseOrNull(expandableRange.range)?.let {
                set.addAll(expandOrgDateTime(it, expandableRange.overdueToday, now, days))
            }
        }

        return set
    }

    /** Used by tests.  */
    fun expandOrgDateTime(rangeStr: String?, now: DateTime, days: Int, overdueToday: Boolean): List<DateTime> {
        return expandOrgDateTime(OrgRange.parse(rangeStr), overdueToday, now, days)
    }

    private fun expandOrgDateTime(
            range: OrgRange, overdueToday: Boolean, now: DateTime, days: Int): List<DateTime> {

        // Only unique values
        val result: MutableSet<DateTime> = LinkedHashSet()

        var rangeStart = range.startTime
        val rangeEnd = range.endTime

        // Add today if task is overdue
        if (overdueToday) {
            if (rangeStart.calendar.before(now.toGregorianCalendar())) {
                result.add(now.withTimeAtStartOfDay())
            }
        }

        var to = now.plusDays(days).withTimeAtStartOfDay()

        if (rangeEnd == null) {
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, now, to, true, 0))

        } else { // a time range
            if (to.isAfter(rangeEnd.calendar.timeInMillis)) {
                to = DateTime(rangeEnd.calendar).withTimeAtStartOfDay().plusDays(1)
            }

            // If start time has no repeater, use a daily repeater
            if (!rangeStart.hasRepeater()) {
                val start = DateTime(rangeStart.calendar)
                rangeStart = buildOrgDateTimeFromDate(start, OrgRepeater.parse("++1d"))
            }

            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, now, to, true, 0))
        }

        return ArrayList(result)
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