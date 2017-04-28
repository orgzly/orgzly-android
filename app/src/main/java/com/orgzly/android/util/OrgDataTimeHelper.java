package com.orgzly.android.util;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pxsalehi on 27.04.17.
 */

public class OrgDataTimeHelper {
    /**
     * return a list of dates where 
     * @param dateStr
     * @param days
     * @return
     */
    public static List<AgendaEntry> expandDateTime(String dateStr, int days) {
        List<AgendaEntry> entries = new LinkedList<>();
        return entries;
    }

    public static class AgendaEntry {
        private Date agendaDate;
        private OrgRange entryScheduled;

        public AgendaEntry(Date agendaDate, OrgRange entryScheduled) {
            this.agendaDate = agendaDate;
            this.entryScheduled = entryScheduled;
        }

        public Date getAgendaDate() {
            return agendaDate;
        }

        public void setAgendaDate(Date agendaDate) {
            this.agendaDate = agendaDate;
        }

        public OrgRange getEntryScheduled() {
            return entryScheduled;
        }

        public void setEntryScheduled(OrgRange entryScheduled) {
            this.entryScheduled = entryScheduled;
        }
    }

    public static OrgDateTime buildFromCal(Calendar cal) {
        return new OrgDateTime.Builder()
                .setYear(cal.get(Calendar.YEAR))
                .setMonth(cal.get(Calendar.MONTH))
                .setDay(cal.get(Calendar.DAY_OF_MONTH))
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE)).build();
    }
}
