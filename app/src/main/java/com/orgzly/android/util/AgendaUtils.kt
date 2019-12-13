package com.orgzly.android.util

import com.orgzly.android.ui.notes.query.agenda.AgendaItems.ExpandableOrgRange
import com.orgzly.org.datetime.*
import org.joda.time.DateTime
import java.util.*

object AgendaUtils {
    fun expandOrgDateTime(range: ExpandableOrgRange, now: DateTime, days: Int): Set<DateTime> {
        return TreeSet<DateTime>().apply {
            addAll(expandOrgDateTime(
                    range.range,
                    range.overdueToday,
                    range.warningPeriod,
                    range.delayPeriod,
                    now,
                    days))
        }
    }

    private fun expandOrgDateTime(
            range: OrgRange,
            overdueToday: Boolean,
            warningPeriod: OrgInterval?,
            delayPeriod: OrgInterval?,
            now: DateTime,
            days: Int
    ): List<DateTime> {

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
            result.addAll(OrgDateTimeUtils.getTimesInInterval(
                    rangeStart, now, to, true, warningPeriod, 0))

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
                    rangeStart, now, to, true, warningPeriod, 0))
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