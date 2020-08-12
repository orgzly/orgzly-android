package com.orgzly.android.reminders

import com.orgzly.android.OrgzlyTest
import com.orgzly.android.db.dao.ReminderTimeDao
import com.orgzly.android.reminders.ReminderService.Companion.getNoteReminders
import org.joda.time.Instant
import org.junit.Assert
import org.junit.Test

class ReminderServiceTest : OrgzlyTest() {
    @Test
    fun testNotesWithTimesSkipDoneState() {
        testUtils.setupBook(
                "notebook",
                """
                    * Note 1
                    SCHEDULED: <2017-03-20>
                    * DONE Note 2
                    SCHEDULED: <2017-03-20>
                    * Note 3
                """.trimIndent())

        val now = Instant.parse("2017-03-15")

        val notes = getNoteReminders(
                context, dataRepository, now, LastRun(), ReminderService.TIME_FROM_NOW)

        Assert.assertEquals(1, notes.size.toLong())
    }

    @Test
    fun testNotesWithTimesWithRepeater() {
        testUtils.setupBook(
                "notebook",
                """
                    * Note 1
                    SCHEDULED: <2017-03-10 Fri +1w>
                    * Note 2
                    SCHEDULED: <2017-03-20 Mon 16:00>
                    * Note 3
                    * Note 4
                    SCHEDULED: <2017-03-16 Thu +1w>
                """.trimIndent())

        val now = Instant.parse("2017-03-15T13:00:00") // Wed

        val notes = getNoteReminders(
                context, dataRepository, now, LastRun(), ReminderService.TIME_FROM_NOW)

        Assert.assertEquals(2, notes.size.toLong())

        notes[0].apply {
            Assert.assertEquals("Note 4", payload.title)
            Assert.assertEquals("2017-03-16T09:00:00.000", runTime.toLocalDateTime().toString())
        }

        notes[1].apply {
            Assert.assertEquals("Note 2", payload.title)
            Assert.assertEquals("2017-03-20T16:00:00.000", runTime.toLocalDateTime().toString())
        }
    }

    @Test
    fun testReminderForDeadlineTime() {
        testUtils.setupBook(
                "notebook",
                """
                    * Note 1
                    SCHEDULED: <2017-03-16 Thu +1w>
                    * Note 2
                    DEADLINE: <2017-03-20 Mon 16:00>
                """.trimIndent())

        val now = Instant.parse("2017-03-15T13:00:00") // Wed

        val notes = getNoteReminders(
                context, dataRepository, now, LastRun(), ReminderService.TIME_FROM_NOW)

        Assert.assertEquals(2, notes.size.toLong())

        notes[0].apply {
            Assert.assertEquals("Note 1", payload.title)
            Assert.assertEquals(ReminderTimeDao.SCHEDULED_TIME, payload.timeType)
            Assert.assertEquals("2017-03-16T09:00:00.000", runTime.toLocalDateTime().toString())
        }

        notes[1].apply {
            Assert.assertEquals("Note 2", payload.title)
            Assert.assertEquals(ReminderTimeDao.DEADLINE_TIME, payload.timeType)
            Assert.assertEquals("2017-03-20T16:00:00.000", runTime.toLocalDateTime().toString())
        }
    }

    @Test
    fun testSchedulingBeforeDailyTime() {
        testUtils.setupBook(
                "notebook",
                """
                    * Note 1
                    SCHEDULED: <2017-03-16 Fri +1w>
                """.trimIndent())

        val now = Instant.parse("2017-03-16T07:00:00")

        val notes = getNoteReminders(
                context, dataRepository, now, LastRun(), ReminderService.TIME_FROM_NOW)

        Assert.assertEquals(1, notes.size.toLong())

        notes[0].apply {
            Assert.assertEquals("Note 1", payload.title)
            Assert.assertEquals(ReminderTimeDao.SCHEDULED_TIME, payload.timeType)
            Assert.assertEquals("2017-03-16T09:00:00.000", runTime.toLocalDateTime().toString())
        }
    }
}