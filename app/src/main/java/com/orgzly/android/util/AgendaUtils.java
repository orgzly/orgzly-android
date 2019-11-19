package com.orgzly.android.util;

import com.orgzly.android.ui.notes.query.agenda.AgendaItems;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.datetime.OrgRepeater;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AgendaUtils {

    public static Set<DateTime> expandOrgDateTime(
            AgendaItems.ExpandableOrgRange[] rangeStrings, DateTime now, int days) {

        Set<DateTime> set = new TreeSet<>();

        for (AgendaItems.ExpandableOrgRange expandableRange : rangeStrings) {
            OrgRange range = OrgRange.parseOrNull(expandableRange.getRange());
            if (range != null) {
                set.addAll(expandOrgDateTime(range, expandableRange.getOverdueToday(), now, days));
            }
        }

        return set;
    }

    /** Used by tests. */
    static List<DateTime> expandOrgDateTime(String rangeStr, Calendar now, int days, boolean overdueToday) {
        return expandOrgDateTime(OrgRange.parse(rangeStr), overdueToday, new DateTime(now), days);
    }

    private static List<DateTime> expandOrgDateTime(
            OrgRange range, boolean overdueToday, DateTime now, int days) {

        // Only unique values
        Set<DateTime> result = new LinkedHashSet<>();

        OrgDateTime rangeStart = range.getStartTime();
        OrgDateTime rangeEnd = range.getEndTime();

        // Add today if task is overdue
        if (overdueToday) {
            if (rangeStart.getCalendar().before(now.toGregorianCalendar())) {
                result.add(now.withTimeAtStartOfDay());
            }
        }

        DateTime to = now.plusDays(days).withTimeAtStartOfDay();

        if (rangeEnd == null) {
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, now, to, true, 0));
        } else {
            // a time range
            if (to.isAfter(rangeEnd.getCalendar().getTimeInMillis())) {
                to = new DateTime(rangeEnd.getCalendar()).withTimeAtStartOfDay().plusDays(1);
            }
            // if start time has no repeater, use a daily repeater
            if (!rangeStart.hasRepeater()) {
                DateTime start = new DateTime(rangeStart.getCalendar());
                rangeStart = buildOrgDateTimeFromDate(start, OrgRepeater.parse("++1d"));
            }
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, now, to, true, 0));
        }

        return new ArrayList<>(result);
    }

    private static OrgDateTime buildOrgDateTimeFromDate(DateTime date, OrgRepeater repeater) {
        OrgDateTime.Builder builder = new OrgDateTime.Builder();

        builder.setYear(date.getYear())
                .setMonth(date.getMonthOfYear() - 1)
                .setDay(date.getDayOfMonth())
                .setHour(date.getHourOfDay())
                .setMinute(date.getMinuteOfHour());

        if (repeater != null)
            builder.setRepeater(repeater);

        return builder.build();
    }
}
