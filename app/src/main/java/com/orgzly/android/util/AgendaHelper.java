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

    public static List<Date> expandOrgRange(String rangeStr, int days) {
        List<Date> entries = new LinkedList<>();
        OrgRange range = OrgRange.getInstanceOrNull(rangeStr);
        if (range == null)
            return entries;
        return expandOrgRange(range, days);
    }
    /**
     * Expand the given OrgRange to dates it occurs on, within the time range [today : today + days]
     * @param range OrgRange object to be expanded
     * @param days  Number of days
     * @return List of dates where this OrgRange occurs
     */
    public static List<Date> expandOrgRange(OrgRange range, int days) {
        if (days < 1)
            throw new IllegalStateException("Agenda must be at least one day long!");
        List<Date> entries = new LinkedList<>();
        Calendar agendaToday = Calendar.getInstance();
        resetJustTime(agendaToday);
        Calendar nextAgendaDay = Calendar.getInstance();
        nextAgendaDay.setTime(agendaToday.getTime());
        nextAgendaDay.add(Calendar.DAY_OF_YEAR, 1);
        Calendar agendaEnd = Calendar.getInstance();
        agendaEnd.add(Calendar.DAY_OF_YEAR, days - 1);
        resetJustTime(agendaEnd);

        OrgDateTime scheduledStart = range.getStartTime();
        Date scheduledStartDate = getJustDate(scheduledStart.getCalendar().getTime());

        if (range.getEndTime() == null) {
            if (!scheduledStart.hasRepeater()) {
                entries.add(scheduledStartDate);
            } else {
                OrgRepeater repeater = scheduledStart.getRepeater();
                Date scheduledDate = scheduledStart.getCalendar().getTime();
                Calendar nextOccur = Calendar.getInstance();
                nextOccur.setTime(scheduledDate);
                // scheduled for today or before today, add to today's agenda
                if (!scheduledStartDate.after(agendaToday.getTime())) {
                    entries.add(agendaToday.getTime());
                    // find next occurrence after today
                    if (repeater.getType() == OrgRepeater.Type.RESTART)
                        repeater.shiftCalendar(nextOccur, Calendar.getInstance());
                    while(nextOccur.before(nextAgendaDay))
                        shiftByInterval(repeater, nextOccur);
                }
                while (!nextOccur.after(agendaEnd)) {
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
            // move to today
            Calendar nextOccur = Calendar.getInstance();
            nextOccur.setTime(nextAgendaDay.getTime());
            while(!nextOccur.after(agendaEnd) && !nextOccur.getTime().after(scheduledEndDate)) {
                entries.add(getJustDate(nextOccur.getTime()));
                nextOccur.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return entries;
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

    public static Date getJustDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        resetJustTime(cal);
        return cal.getTime();
    }

    public static void resetJustTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    public static OrgDateTime buildFromCal(Calendar cal) {
        return new OrgDateTime.Builder()
                .setYear(cal.get(Calendar.YEAR))
                .setMonth(cal.get(Calendar.MONTH))
                .setDay(cal.get(Calendar.DAY_OF_MONTH))
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE)).build();
    }

    public static void main(String[] args) {
        String range = "<2017-05-03 Wed>--<2017-05-11 Do>";
        List<Date> rangeDates = expandOrgRange(range, 5);
        System.out.println(OrgStringUtils.join(rangeDates, ", "));

        String date = "<2017-05-02 Tue ++3d>";
        List<Date> dateDates = expandOrgRange(date, 5);
        System.out.println(OrgStringUtils.join(dateDates, ", "));

        String single = "<2017-05-04 Do>";
        List<Date> singleDates = expandOrgRange(single, 5);
        System.out.println(OrgStringUtils.join(singleDates, ", "));

        String hourly = "<2017-05-03 Wed 09:00 ++12h>";
        List<Date> hourlyDates = expandOrgRange(hourly, 5);
        System.out.println(OrgStringUtils.join(hourlyDates, ", "));

        String hourlyRestart = "<2017-05-03 Wed 09:00 .+12h>";
        List<Date> hourlyRestartDates = expandOrgRange(hourlyRestart, 5);
        System.out.println(OrgStringUtils.join(hourlyRestartDates, ", "));

        String weeklyCumulate = "<2017-05-06 Sat +1w>";
        List<Date> weeklyCumulateDates = expandOrgRange(weeklyCumulate, 10);
        System.out.println(OrgStringUtils.join(weeklyCumulateDates, ", "));
    }
}
