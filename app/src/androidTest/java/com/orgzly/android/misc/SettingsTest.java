package com.orgzly.android.misc;

import com.orgzly.android.Note;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class SettingsTest extends OrgzlyTest {
    @Test
    public void testStateChangeAndNotesReparse() throws IOException {
        Note note;

        shelfTestUtils.setupBook("booky", "* TODO [#A] Title");

        AppPreferences.states(context, "TODO|DONE");
        shelf.reParseNotesStateAndTitles();

        note = shelf.getNote(1);
        assertEquals("TODO", note.getHead().getState());
        assertEquals("A", note.getHead().getPriority());
        assertEquals("Title", note.getHead().getTitle());

        AppPreferences.states(context, "");
        shelf.reParseNotesStateAndTitles();

        note = shelf.getNote(1);
        assertNull(note.getHead().getState());
        assertNull(note.getHead().getPriority());
        assertEquals("TODO [#A] Title", note.getHead().getTitle());

        AppPreferences.states(context, "TODO|DONE");
        shelf.reParseNotesStateAndTitles();

        note = shelf.getNote(1);
        assertEquals("TODO", note.getHead().getState());
        assertEquals("A", note.getHead().getPriority());
        assertEquals("Title", note.getHead().getTitle());
    }
}
