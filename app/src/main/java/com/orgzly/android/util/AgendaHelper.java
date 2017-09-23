package com.orgzly.android.util;

import com.orgzly.org.OrgStringUtils;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDateTimeUtils;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.datetime.OrgRepeater;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pxsalehi on 27.04.17.
 */

public class AgendaHelper {

    public static List<Date> expandOrgRange(String rangeStr, Calendar now, int days) {
        List<Date> entries = new LinkedList<>();
        OrgRange range = OrgRange.parseOrNull(rangeStr);
        if (range == null)
            return entries;
        return expandOrgRange(range, now, days);
    }
    /**
     * Expand the given OrgRange to dates it occurs on, within the time range [today : today + days]
     * @param range OrgRange object to be expanded
     * @param days  Number of days
     * @return List of dates where this OrgRange occurs
     */
    public static List<Date> expandOrgRange(OrgRange range, Calendar now, int days) {
        if (days < 1)
            throw new IllegalStateException("Agenda must be at least one day long!");
        List<Date> entries = new LinkedList<>();
        Calendar agendaToday = Calendar.getInstance();
        agendaToday.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        agendaToday.set(Calendar.MILLISECOND, 0);
        Calendar nextAgendaDay = Calendar.getInstance();
        nextAgendaDay.setTime(agendaToday.getTime());
        nextAgendaDay.add(Calendar.DAY_OF_YEAR, 1);
        Calendar agendaEnd = Calendar.getInstance();
        agendaEnd.setTime(agendaToday.getTime());
        agendaEnd.add(Calendar.DAY_OF_YEAR, days);
        resetJustTime(agendaEnd);

        OrgDateTime scheduledStart = range.getStartTime();
        Date scheduledStartDate = getJustDate(scheduledStart.getCalendar().getTime());

        if (range.getEndTime() == null) {
            if (!scheduledStart.hasRepeater()) {
                // no repeater, no range
                if (!scheduledStartDate.after(agendaToday.getTime()))
                    entries.add(agendaToday.getTime());
                else
                    entries.add(scheduledStartDate);
            } else {
                OrgRepeater repeater = scheduledStart.getRepeater();
                Calendar nextOccur = Calendar.getInstance();
                nextOccur.setTime(scheduledStart.getCalendar().getTime());
                // scheduled for today or before today, add to today's agenda
                if (!scheduledStartDate.after(agendaToday.getTime())) {
                    entries.add(agendaToday.getTime());
                    // find next occurrence after today
                    if (repeater.getType() == OrgRepeater.Type.RESTART)
                        repeater.shiftCalendar(nextOccur, now);
                    while(nextOccur.before(nextAgendaDay))
                        shiftByInterval(repeater, nextOccur);
                } else {
                    // scheduled for after today, move agenda day
                    while (nextAgendaDay.getTime().before(scheduledStartDate)) {
                        nextAgendaDay.add(Calendar.DAY_OF_YEAR, 1);
                    }
                }
                while (nextOccur.before(agendaEnd)) {
                    entries.add(getJustDate(nextOccur.getTime()));
                    nextAgendaDay.add(Calendar.DAY_OF_YEAR, 1);
                    do {
                        shiftByInterval(repeater, nextOccur);
                    } while (nextOccur.before(nextAgendaDay));
                }
            }
        } else {
            // a range
            OrgDateTime scheduledEnd = range.getEndTime();
            Date scheduledEndDate = getJustDate(scheduledEnd.getCalendar().getTime());
            Calendar nextOccur = Calendar.getInstance();
            nextOccur.setTime(scheduledStart.getCalendar().getTime());
            if (scheduledStartDate.before(nextAgendaDay.getTime())) {
                // add today and move to tomorrow
                entries.add(agendaToday.getTime());
                nextOccur.setTime(nextAgendaDay.getTime());
            }
            while(nextOccur.before(agendaEnd) && !nextOccur.getTime().after(scheduledEndDate)) {
                entries.add(getJustDate(nextOccur.getTime()));
                nextOccur.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return entries;
    }

    public static List<DateTime> expandOrgDateTime(String rangeStr, Calendar now, int days) {
        List<DateTime> result = new ArrayList<>();
        OrgRange range = OrgRange.parseOrNull(rangeStr);
        if (range == null)
            return result;
        return expandOrgDateTime(range, now, days);
    }

    public static List<DateTime> expandOrgDateTime(OrgRange range, Calendar now, int days) {
        List<DateTime> result = new ArrayList<>();
        DateTime from = new DateTime(now.getTime());
        OrgDateTime rangeStart = range.getStartTime();
        OrgDateTime rangeEnd = range.getEndTime();

        // add an entry for overdue item
        if (rangeStart.getCalendar().before(from.toGregorianCalendar()))
            result.add(DateTime.now().withTimeAtStartOfDay());

        if (rangeEnd == null) {
            DateTime to = from.plusDays(days).withTimeAtStartOfDay();
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, from, to, true, 100));
        } else {
            // a time range
            DateTime to = new DateTime(rangeEnd.getCalendar().getTime()).withTimeAtStartOfDay();
            // if start time has no repeater, use a daily repeater
            if (!rangeStart.hasRepeater()) {
                Calendar start = rangeStart.getCalendar();
                rangeStart = new OrgDateTime.Builder()
                        .setYear(start.get(Calendar.YEAR))
                        .setMonth(start.get(Calendar.MONTH))
                        .setDay(start.get(Calendar.DAY_OF_YEAR))
                        .setHour(start.get(Calendar.HOUR_OF_DAY))
                        .setMinute(start.get(Calendar.MINUTE))
                        .setRepeater(OrgRepeater.parse("++1d"))
                        .build();

            }
            result.addAll(OrgDateTimeUtils.getTimesInInterval(rangeStart, from, to, true, 100));
        }

        return result;
    }

    public static Calendar getTodayDate() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    public static void shiftByInterval(OrgRepeater orgRepeater, Calendar cal) {
        switch (orgRepeater.getUnit()) {
            case HOUR:
                cal.add(Calendar.HOUR_OF_DAY, orgRepeater.getValue());
                break;
            case DAY:
                cal.add(Calendar.DATE, orgRepeater.getValue());
                break;
            case WEEK:
                cal.add(Calendar.WEEK_OF_YEAR, orgRepeater.getValue());
                break;
            case MONTH:
                cal.add(Calendar.MONTH, orgRepeater.getValue());
                break;
            case YEAR:
                cal.add(Calendar.YEAR, orgRepeater.getValue());
                break;
        }
    }

    private static Date getJustDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        resetJustTime(cal);
        return cal.getTime();
    }

    private static void resetJustTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    public static OrgDateTime buildOrgDateTimeFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return buildOrgDateTimeFromCal(cal);
    }

    public static OrgDateTime buildOrgDateTimeFromCal(Calendar cal) {
        return new OrgDateTime.Builder()
                .setYear(cal.get(Calendar.YEAR))
                .setMonth(cal.get(Calendar.MONTH))
                .setDay(cal.get(Calendar.DAY_OF_MONTH))
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE)).build();
    }

    public static void main(String[] args) {
        String orgRangeStr = "<2017-05-03 Wed>--<2017-05-11 Do>";
        List<Date> dates = AgendaHelper.expandOrgRange(orgRangeStr, Calendar.getInstance(), 5);
        for(Date d: dates)
            System.out.println(d);

        System.out.println();

//        OrgDateTime orgDate = OrgDateTime.parseOrNull(orgRangeStr);
//        DateTime from = DateTime.now();
//        DateTime to = from.plusDays(2).withTime(0, 0, 0, 0);
//        List<DateTime> ds = new ArrayList<>();
//        if (orgDate.getCalendar().before(from.toGregorianCalendar()))
//            ds.add(DateTime.now().withTime(0, 0, 0, 0));
//        ds.addAll(OrgDateTimeUtils.getTimesInInterval(orgDate, from, to, true, 1000));
        List<DateTime> ds = expandOrgDateTime(orgRangeStr, Calendar.getInstance(), 2);
        for(DateTime d: ds)
            System.out.println(d);
    }
}
