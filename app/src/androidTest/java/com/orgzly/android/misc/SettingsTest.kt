package com.orgzly.android.misc

import com.orgzly.android.OrgzlyTest
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.note.NoteBuilder
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
        testUtils.setupBook("booky", "* TODO [#A] Title")

        AppPreferences.states(context, "TODO|DONE")
        assertEquals(0, dataRepository.reParseNotesStateAndTitles())

        dataRepository.getLastNote("Title").let {
            assertEquals("TODO", it?.state)
            assertEquals("A", it?.priority)
        }

        AppPreferences.states(context, "")
        assertEquals(1, dataRepository.reParseNotesStateAndTitles())

        dataRepository.getLastNote("TODO [#A] Title").let {
            assertNull(it?.state)
            assertNull(it?.priority)
        }

        AppPreferences.states(context, "TODO|DONE")
        assertEquals(1, dataRepository.reParseNotesStateAndTitles())

        dataRepository.getLastNote("Title").let {
            assertEquals("TODO", it?.state)
            assertEquals("A", it?.priority)
        }
    }

    @Test
    @Throws(IOException::class)
    fun testStarInContent() {
        testUtils.setupBook("booky", "* TODO [#A] Title")

        dataRepository.getLastNoteView("Title")?.let { noteView ->
            val payload = NoteBuilder.newPayload(noteView, emptyList())
                    .copy(content = "Content\n* with star\nin the middle")

            dataRepository.updateNote(noteView.note.id, payload)
        }

        assertEquals(0, dataRepository.reParseNotesStateAndTitles())

        dataRepository.getLastNoteView("Title")?.let {
            assertEquals("TODO", it.note.state)
            assertEquals("A", it.note.priority)
            assertEquals("Title", it.note.title)
        }
    }
}
