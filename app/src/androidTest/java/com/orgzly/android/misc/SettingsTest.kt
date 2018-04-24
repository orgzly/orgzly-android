package com.orgzly.android.misc

import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 *
 */
class SettingsTest : OrgzlyTest() {
    @Test
    @Throws(IOException::class)
    fun testStateChangeAndNotesReparse() {
        shelfTestUtils.setupBook("booky", "* TODO [#A] Title")

        AppPreferences.states(context, "TODO|DONE")
        shelf.reParseNotesStateAndTitles()

        shelf.getNote(1).let { note ->
            assertEquals("TODO", note.head.state)
            assertEquals("A", note.head.priority)
            assertEquals("Title", note.head.title)
        }

        AppPreferences.states(context, "")
        shelf.reParseNotesStateAndTitles()

        shelf.getNote(1).let { note ->
            assertNull(note.head.state)
            assertNull(note.head.priority)
            assertEquals("TODO [#A] Title", note.head.title)
        }

        AppPreferences.states(context, "TODO|DONE")
        shelf.reParseNotesStateAndTitles()

        shelf.getNote(1).let { note ->
            assertEquals("TODO", note.head.state)
            assertEquals("A", note.head.priority)
            assertEquals("Title", note.head.title)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testStarInContent() {
        shelfTestUtils.setupBook("booky", "* TODO [#A] Title")

        shelf.getNote(1).let { note ->
            note.head.content = "Content\n* with star\nin the middle"
            shelf.updateNote(note)
        }

        shelf.reParseNotesStateAndTitles()

        shelf.getNote(1).let {
            assertEquals("TODO", it.head.state)
            assertEquals("A", it.head.priority)
            assertEquals("Title", it.head.title)
        }
    }
}
