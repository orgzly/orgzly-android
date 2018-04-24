package com.orgzly.android.misc

import android.support.test.rule.ActivityTestRule
import com.orgzly.android.Book
import com.orgzly.android.NotesExporter
import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.MainActivity
import com.orgzly.org.datetime.OrgDateTime
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.StringWriter

class StateChangeTest : OrgzlyTest() {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    override fun setUp() {
        super.setUp()

        AppPreferences.logOnTimeShift(context, true)
    }

    /**
     * Tests:
     * - Updated scheduled time
     * - Updated LAST_REPEAT
     * - Kept order of properties
     * - Added state to LOGBOOK.
     */
    @Test
    fun testRecordedStateChangeOnTimeShift() {
        val book = shelfTestUtils.setupBook(
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

        val note = shelf.getNote("Task")

        shelf.setStateToFirstDone(note.id)

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

        assertThat(exportedBook, `is`(expectedBook))
    }

    @Test
    fun testNoContent() {
        val book = shelfTestUtils.setupBook(
                "book-a",
                "* NEXT Task\n" +
                "SCHEDULED: <2018-04-12 Thu +4d/5d>")

        val note = shelf.getNote("Task")

        shelf.setStateToFirstDone(note.id)

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

        assertThat(exportedBook, `is`(expectedBook))
    }

    private fun exportBook(book: Book): String {
        val sw = StringWriter()

        NotesExporter.getInstance(context).exportBook(book, sw)

        return sw.toString()
    }
}
