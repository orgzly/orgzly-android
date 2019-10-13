package com.orgzly.android.misc

import com.orgzly.android.NotesOrgExporter
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.db.entity.BookView
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.org.datetime.OrgDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.StringWriter

class StateChangeTest : OrgzlyTest() {
    @Before
    override fun setUp() {
        super.setUp()

        AppPreferences.logOnTimeShift(context, true)
    }

    /**
     * Checks for:
     * - Updated scheduled time
     * - Updated LAST_REPEAT
     * - Kept order of properties
     * - Added state to LOGBOOK.
     */
    @Test
    fun testRecordedStateChangeOnTimeShift() {
        val book = testUtils.setupBook(
                "book-a",
                "* NEXT Task\n" +
                "SCHEDULED: <2018-04-12 Thu +4d/5d>\n" +
                ":PROPERTIES:\n" +
                ":LAST_REPEAT: [2018-04-08 Sun 09:00]\n" +
                ":STYLE:    habit\n" +
                ":ID:       6BAC58E2-0596-459D-8724-FE158DA00DC0\n" +
                ":END:\n" +
                ":LOGBOOK:\n" +
                "- State \"DONE\"       from \"NEXT\"       [2018-04-08 Sun 09:00]\n" +
                ":END:\n" +
                "\n" +
                "Content")

        val note = dataRepository.getLastNote("Task")

        assertNotNull(note)

        dataRepository.toggleNotesState(setOf(note!!.id))

        val exportedBook = exportBook(book)

        val now = OrgDateTime(false).toString()
        val expectedBook = "* NEXT Task\n" +
                           "SCHEDULED: <2018-04-16 Mon +4d/5d>\n" +
                           ":PROPERTIES:\n" +
                           ":LAST_REPEAT: " + now + "\n" +
                           ":STYLE:    habit\n" +
                           ":ID:       6BAC58E2-0596-459D-8724-FE158DA00DC0\n" +
                           ":END:\n" +
                           ":LOGBOOK:\n" +
                           "- State \"DONE\"       from \"NEXT\"       " + now + "\n" +
                           "- State \"DONE\"       from \"NEXT\"       [2018-04-08 Sun 09:00]\n" +
                           ":END:\n" +
                           "\n" +
                           "Content\n\n"

        assertEquals(expectedBook, exportedBook)
    }

    @Test
    fun testNoContent() {
        val book = testUtils.setupBook(
                "book-a",
                "* NEXT Task\n" +
                "SCHEDULED: <2018-04-12 Thu +4d/5d>")

        val note = dataRepository.getLastNote("Task")

        assertNotNull(note)

        dataRepository.toggleNotesState(setOf(note!!.id))

        val exportedBook = exportBook(book)

        val now = OrgDateTime(false).toString()
        val expectedBook = "* NEXT Task\n" +
                           "SCHEDULED: <2018-04-16 Mon +4d/5d>\n" +
                           ":PROPERTIES:\n" +
                           ":LAST_REPEAT: " + now + "\n" +
                           ":END:\n" +
                           ":LOGBOOK:\n" +
                           "- State \"DONE\"       from \"NEXT\"       " + now + "\n" +
                           ":END:\n" +
                           "\n"

        assertEquals(expectedBook, exportedBook)
    }

    @Test
    fun testRemoveScheduledTimeAfterShiftingDeadline() {
        AppPreferences.setLastRepeatOnTimeShift(context, false)
        AppPreferences.logOnTimeShift(context, false)

        val book = testUtils.setupBook(
                "book-a",
                "* TODO Task\n" +
                "  DEADLINE: <2013-08-10 Sat +1w> SCHEDULED: <2013-08-08 Thu>")

        val note = dataRepository.getLastNote("Task")

        dataRepository.toggleNotesState(setOf(note!!.id))

        val exportedBook = exportBook(book)

        val expectedBook = "* TODO Task\n" +
                           "  DEADLINE: <2013-08-17 Sat +1w>\n\n"

        assertEquals(expectedBook, exportedBook)
    }

    private fun exportBook(book: BookView): String {
        val sw = StringWriter()

        NotesOrgExporter(dataRepository).exportBook(book.book, sw)

        return sw.toString()
    }
}
