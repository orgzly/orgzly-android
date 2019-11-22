package com.orgzly.android.reminders;

import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.dao.ReminderTimeDao;
import com.orgzly.android.prefs.AppPreferences;

import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ReminderServiceTest extends OrgzlyTest {
    @Test
    public void testNotesWithTimesSkipDoneState() {
        testUtils.setupBook("notebook",
                "* Note 1\n"+
                "SCHEDULED: <2017-03-20>\n" +
                "* DONE Note 2\n"+
                "SCHEDULED: <2017-03-20>\n" +
                "* Note 3");

        LastRun lastRun = new LastRun();
        Instant now = Instant.parse("2017-03-15");
        AppPreferences.remindersForScheduledEnabled(context, true);

        List<NoteReminder> notes = ReminderService.getNoteReminders(
                context, dataRepository, now, lastRun, ReminderService.TIME_FROM_NOW);

        assertEquals(1, notes.size());
    }

    @Test
    public void testNotesWithTimesWithRepeater() {
        testUtils.setupBook("notebook",
                "* Note 1\n"+
                "SCHEDULED: <2017-03-10 Fri +1w>\n" +
                "* Note 2\n"+
                "SCHEDULED: <2017-03-20 Mon 16:00>\n" +
                "* Note 3\n" +
                "* Note 4\n"+
                "SCHEDULED: <2017-03-16 Fri +1w>\n");

        LastRun lastRun = new LastRun();
        Instant now = Instant.parse("2017-03-15T13:00:00"); // Wed
        AppPreferences.remindersForScheduledEnabled(context, true);

        List<NoteReminder> notes = ReminderService.getNoteReminders(
                context, dataRepository, now, lastRun, ReminderService.TIME_FROM_NOW);

        assertEquals(2, notes.size());

        assertEquals("Note 4", notes.get(0).getPayload().title);
        assertEquals("2017-03-16T09:00:00", new LocalDateTime(notes.get(0).getRunTime()).toString("yyyy-MM-dd'T'HH:mm:ss"));

        assertEquals("Note 2", notes.get(1).getPayload().title);
        assertEquals("2017-03-20T16:00:00", new LocalDateTime(notes.get(1).getRunTime()).toString("yyyy-MM-dd'T'HH:mm:ss"));
    }

    @Test
    public void testReminderForDeadlineTime() {
        testUtils.setupBook("notebook",
                "* Note 1\n"+
                "SCHEDULED: <2017-03-16 Fri +1w>\n" +
                "* Note 2\n"+
                "DEADLINE: <2017-03-20 Mon 16:00>\n");

        LastRun lastRun = new LastRun();
        Instant now = Instant.parse("2017-03-15T13:00:00"); // Wed
        AppPreferences.remindersForDeadlineEnabled(context, true);

        List<NoteReminder> notes = ReminderService.getNoteReminders(
                context, dataRepository, now, lastRun, ReminderService.TIME_FROM_NOW);

        assertEquals(1, notes.size());

        NoteReminder reminder = notes.get(0);
        assertEquals("Note 2", reminder.getPayload().title);
        assertEquals(ReminderTimeDao.DEADLINE_TIME, reminder.getPayload().timeType);
        assertEquals("2017-03-20T16:00:00", new LocalDateTime(reminder.getRunTime()).toString("yyyy-MM-dd'T'HH:mm:ss"));
    }
}