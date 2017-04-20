package com.orgzly.android.reminders;

import com.orgzly.android.OrgzlyTest;

import org.joda.time.Instant;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

public class ReminderServiceTest extends OrgzlyTest {
    @Test
    public void testNotesWithTimesSkipDoneState() {
        shelfTestUtils.setupBook("notebook",
                "* Note 1\n"+
                "SCHEDULED: <2017-03-20>\n" +
                "* DONE Note 2\n"+
                "SCHEDULED: <2017-03-20>\n" +
                "* Note 3");

        long now = Instant.parse("2017-03-15").getMillis();

        List<ReminderService.NoteWithTime> notes = ReminderService.getNotesWithTimes(context, 0, now);

        assertEquals(1, notes.size());
    }

//    @Test
//    public void testNotesWithTimesWithRepeater() {
//        shelfTestUtils.setupBook("notebook",
//                "* Note 1\n"+
//                "SCHEDULED: <2017-03-10 +1w>\n" +
//                "* Note 2\n"+
//                "SCHEDULED: <2017-03-20 16:00>\n" +
//                "* Note 3");
//
//        long now = Instant.parse("2017-03-15T13:00:00").getMillis();
//
//        List<ReminderService.NoteWithTime> notes = ReminderService.getNotesWithTimes(context, now, now);
//
//        assertEquals(2, notes.size());
//
//        assertEquals("Note 1", notes.get(0).title);
//        assertEquals("2017-03-17T09:00:00", new LocalDateTime(notes.get(0).time).toString("yyyy-MM-dd'T'HH:mm:ss"));
//
//        assertEquals("Note 2", notes.get(1).title);
//        assertEquals("2017-03-20T16:00:00", new LocalDateTime(notes.get(1).time).toString("yyyy-MM-dd'T'HH:mm:ss"));
//    }
}