package com.orgzly.android.util;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.datetime.OrgRepeater;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AgendaUtils {

    /**
     * This method returns only one {@link DateTime} per day
     * to avoid displaying the same note multiple times.
     */
    public static Set<DateTime> expandOrgDateTime(String[] rangeStrings, DateTime now, int days) {
        Set<DateTime> set = new TreeSet<>();

        for (String rangeString : rangeStrings) {
            OrgRange range = OrgRange.parseOrNull(rangeString);
            if (range != null) {
                set.addAll(expandOrgDateTime(range, now, days));
            }
        }

        /* Truncate and remove duplicates. */
        Set<DateTime> result = new TreeSet<>();
        for (DateTime dt: set) {
            result.add(dt.withTimeAtStartOfDay());
        }

        return result;
    }

    public static List<DateTime> expandOrgDateTime(String rangeStr, Calendar now, int days) {
        OrgRange range = OrgRange.parseOrNull(rangeStr);
        if (range == null) {
            return new ArrayList<>();
        }
        return expandOrgDateTime(range, new DateTime(now), days);
    }

    private static List<DateTime> expandOrgDateTime(OrgRange range, DateTime now, int days) {
        List<DateTime> result = new ArrayList<>();
        OrgDateTime rangeStart = range.getStartTime();
        OrgDateTime rangeEnd = range.getEndTime();

        // Add today if task is overdue
        if (rangeStart.getCalendar().before(now.toGregorianCalendar())) {
            result.add(now.withTimeAtStartOfDay());
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

        return result;
    }

    private static OrgDateTime buildOrgDateTimeFromDate(DateTime date, OrgRepeater repeater) {
        OrgDateTime.Builder builder = new OrgDateTime.Builder();

        builder.setYear(date.getYear())
                .setMonth(date.getMonthOfYear() - 1)
                .setDay(date.getDayOfMonth())
                .setHour(date.getHourOfDay())
                .setMinute(date.getMinuteOfHour());

        if (repeater != null)
            builder.setHasRepeater(true).setRepeater(repeater);

        return builder.build();
    }
}
