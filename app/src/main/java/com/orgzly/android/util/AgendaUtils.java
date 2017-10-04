package com.orgzly.android.util;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.datetime.OrgRepeater;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AgendaUtils {

    public static List<DateTime> expandOrgDateTime(String rangeStr, Calendar now, int days) {
        List<DateTime> result = new ArrayList<>();
        OrgRange range = OrgRange.parseOrNull(rangeStr);
        if (range == null)
            return result;
        return expandOrgDateTime(range, now, days);
    }

    private static List<DateTime> expandOrgDateTime(OrgRange range, Calendar now, int days) {
        List<DateTime> result = new ArrayList<>();
        DateTime from = new DateTime(now.getTime());
        OrgDateTime rangeStart = range.getStartTime();
        OrgDateTime rangeEnd = range.getEndTime();

        // add a today entry task is overdue
        if (rangeStart.getCalendar().before(from.toGregorianCalendar()))
            result.add(new DateTime(now).withTimeAtStartOfDay());

        DateTime to = from.plusDays(days).withTimeAtStartOfDay();
        if (rangeEnd == null) {
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, from, to, true, 100));
        } else {
            // a time range
            if (to.isAfter(rangeEnd.getCalendar().getTimeInMillis()))
                to = new DateTime(rangeEnd.getCalendar()).withTimeAtStartOfDay().plusDays(1);
            // if start time has no repeater, use a daily repeater
            if (!rangeStart.hasRepeater()) {
                DateTime start = new DateTime(rangeStart.getCalendar());
                rangeStart = buildOrgDateTimeFromDate(start, OrgRepeater.parse("++1d"));
            }
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, from, to, true, 100));
        }

        return result;
    }

    public static OrgDateTime buildOrgDateTimeFromDate(DateTime date, OrgRepeater repeater) {
        OrgDateTime.Builder builder =  new OrgDateTime.Builder();

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
