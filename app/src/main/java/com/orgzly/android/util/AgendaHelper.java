package com.orgzly.android.util;

import com.orgzly.org.OrgStringUtils;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.datetime.OrgRepeater;

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
        OrgRange range = OrgRange.getInstanceOrNull(rangeStr);
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
}
