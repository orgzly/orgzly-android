package com.orgzly.android.misc;

import android.database.Cursor;

import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.Note;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.org.OrgHead;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class StructureTest extends OrgzlyTest {
    @Test
    public void testNewNote() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n");

        Note newNote = new Note();
        newNote.getPosition().setBookId(book.getId());
        newNote.getHead().setTitle("Note 2");

        shelf.createNote(newNote, null);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "* Note 2\n"
                ,
                shelf.getBookContent("notebook", BookName.Format.ORG));

        assertEquals(shelf.getNote("").getId(), shelf.getNote("Note 2").getPosition().getParentId());

    }

    @Test
    public void testBookSetupLevels() {
        shelfTestUtils.setupBook("notebook", "" +
                                             "description\n" +
                                             "* Note #1.\n" +
                                             "* Note #2.\n" +
                                             "** Note #3.\n" +
                                             "** Note #4.\n" +
                                             "*** Note #5.\n" +
                                             "**** Note #6.\n" +
                                             "** Note #7.\n" +
                                             "* Note #8.\n" +
                                             "**** Note #9.\n" +
                                             "** Note #10.\n" +
                                             "");

        assertEquals(0, shelf.getNote("").getPosition().getLevel());
        assertEquals(1, shelf.getNote("Note #1.").getPosition().getLevel());
        assertEquals(1, shelf.getNote("Note #2.").getPosition().getLevel());
        assertEquals(2, shelf.getNote("Note #3.").getPosition().getLevel());
        assertEquals(2, shelf.getNote("Note #4.").getPosition().getLevel());
        assertEquals(3, shelf.getNote("Note #5.").getPosition().getLevel());
        assertEquals(4, shelf.getNote("Note #6.").getPosition().getLevel());
        assertEquals(2, shelf.getNote("Note #7.").getPosition().getLevel());
        assertEquals(1, shelf.getNote("Note #8.").getPosition().getLevel());
        assertEquals(4, shelf.getNote("Note #9.").getPosition().getLevel());
        assertEquals(2, shelf.getNote("Note #10.").getPosition().getLevel());
    }

    @Test
    public void testCut() {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "* Note #1.\n" +
                                                         "* Note #2.\n" +
                                                         "** Note #3.\n" +
                                                         "** Note #4.\n" +
                                                         "*** Note #5.\n" +
                                                         "**** Note #6.\n" +
                                                         "** Note #7.\n" +
                                                         "* Note #8.\n" +
                                                         "**** Note #9.\n" +
                                                         "** Note #10.\n" +
                                                         "");

        Set<Long> ids = new HashSet<>();
        ids.add(shelf.getNote("Note #1.").getId());
        ids.add(shelf.getNote("Note #3.").getId());

        shelf.cut(book.getId(), ids);

        assertEquals("There should be less notes in the book", 8, NotesClient.getCount(context, book.getId()));

        Cursor cursor = NotesClient.getCursorForBook(context, book.getName());
        try {
            Note note;
            OrgHead head;

            cursor.moveToFirst();
            note = NotesClient.fromCursor(cursor);
            head = note.getHead();
            assertEquals("Title for book should match", "Note #2.", head.getTitle());
            assertEquals("Level for book should match", 1, note.getPosition().getLevel());

            cursor.moveToNext();
            note = NotesClient.fromCursor(cursor);
            head = note.getHead();
            assertEquals("Title for book should match", "Note #4.", head.getTitle());
            assertEquals("Level for book should match", 2, note.getPosition().getLevel());

            cursor.moveToNext();
            note = NotesClient.fromCursor(cursor);
            head = note.getHead();
            assertEquals("Title for book should match", "Note #5.", head.getTitle());
            assertEquals("Level for book should match", 3, note.getPosition().getLevel());

        } finally {
            cursor.close();
        }
    }

    @Test
    public void testPasteToDifferentBook() throws IOException {
        shelfTestUtils.setupBook("notebook-1", "" +
                                               "description\n" +
                                               "\n" +
                                               "* Note 1\n" +
                                               "** Note 1.1\n" +
                                               "*** Note 1.1.1\n");

        Book book2 = shelfTestUtils.setupBook("notebook-2", "" +
                                                            "description\n" +
                                                            "\n" +
                                                            "* Note A\n" +
                                                            "** Note A.A\n" +
                                                            "*** Note A.A.A\n");

        shelf.cut(book2.getId(), shelf.getNote("Note A.A").getId());

        Note n = shelf.getNote("Note 1.1.1");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.UNDER);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "*** Note 1.1.1\n" +
                     "**** Note A.A\n" +
                     "***** Note A.A.A\n",
                shelf.getBookContent("notebook-1", BookName.Format.ORG));

        assertEquals("description\n" +
                     "\n" +
                     "* Note A\n",
                shelf.getBookContent("notebook-2", BookName.Format.ORG));
    }

    @Test
    public void testPasteUnder() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "** Note 1.2\n" +
                                                         "*** Note 1.2.1\n" +
                                                         "*** Note 1.2.2\n" +
                                                         "* Note 2\n");

        /* Cut & paste 1.1 under 1.2. */
        shelf.cut(book.getId(), shelf.getNote("Note 1.1").getId());
        Note n = shelf.getNote("Note 1.2");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.UNDER);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.2\n" +
                     "*** Note 1.2.1\n" +
                     "*** Note 1.2.2\n" +
                     "*** Note 1.1\n" +
                     "**** Note 1.1.1\n" +
                     "* Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n12 = shelf.getNote("Note 1.2").getPosition();
        NotePosition n121 = shelf.getNote("Note 1.2.1").getPosition();
        NotePosition n122 = shelf.getNote("Note 1.2.2").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n111 = shelf.getNote("Note 1.1.1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();

        assertEquals(shelf.getNote("Note 1.2").getId(), n11.getParentId());

        assertEquals(1, n1.getLevel());
        assertEquals(2, n12.getLevel());
        assertEquals(3, n121.getLevel());
        assertEquals(3, n122.getLevel());
        assertEquals(3, n11.getLevel());
        assertEquals(4, n111.getLevel());
        assertEquals(1, n2.getLevel());

        assertTrue(n1.getLft() < n12.getLft());
        assertTrue(n12.getLft() < n121.getLft());
        assertTrue(n121.getLft() < n121.getRgt());
        assertTrue(n121.getRgt() < n122.getLft());
        assertTrue(n122.getLft() < n122.getRgt());
        assertTrue(n122.getRgt() < n11.getLft());
        assertTrue(n11.getLft() < n111.getLft());
        assertTrue(n111.getLft() < n111.getRgt());
        assertTrue(n111.getRgt() < n11.getRgt());
        assertTrue(n11.getRgt() < n12.getRgt());
        assertTrue(n12.getRgt() < n1.getRgt());
        assertTrue(n1.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());

        assertEquals(5, n1.getDescendantsCount());
        assertEquals(4, n12.getDescendantsCount());
        assertEquals(0, n121.getDescendantsCount());
        assertEquals(0, n122.getDescendantsCount());
        assertEquals(1, n11.getDescendantsCount());
        assertEquals(0, n111.getDescendantsCount());
        assertEquals(0, n2.getDescendantsCount());
    }

    @Test
    public void testPasteUnderFolded() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "*** Note 1.1.2\n" +
                                                         "** Note 1.2\n" +
                                                         "* Note 2\n");

        /* Cut & paste 2 under folded 1.1. */
        shelf.cut(book.getId(), shelf.getNote("Note 2").getId());
        shelf.toggleFoldedState(shelf.getNote("Note 1.1").getId());
        Note n = shelf.getNote("Note 1.1");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.UNDER);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "*** Note 1.1.1\n" +
                     "*** Note 1.1.2\n" +
                     "*** Note 2\n" +
                     "** Note 1.2\n"
                ,
                shelf.getBookContent("notebook", BookName.Format.ORG));


        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n111 = shelf.getNote("Note 1.1.1").getPosition();
        NotePosition n112 = shelf.getNote("Note 1.1.2").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();
        NotePosition n12 = shelf.getNote("Note 1.2").getPosition();

        assertEquals(shelf.getNote("Note 1.1").getId(), n2.getParentId());

        assertEquals(1, n1.getLevel());
        assertEquals(2, n11.getLevel());
        assertEquals(3, n111.getLevel());
        assertEquals(3, n112.getLevel());
        assertEquals(3, n2.getLevel());
        assertEquals(2, n12.getLevel());

        assertTrue(n1.getLft() < n11.getLft());
        assertTrue(n11.getLft() < n111.getLft());
        assertTrue(n111.getLft() < n111.getRgt());
        assertTrue(n111.getLft() < n111.getRgt());
        assertTrue(n111.getRgt() < n112.getLft());
        assertTrue(n112.getLft() < n112.getRgt());
        assertTrue(n112.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
        assertTrue(n2.getRgt() < n11.getRgt());
        assertTrue(n11.getRgt() < n12.getLft());
        assertTrue(n12.getLft() < n12.getRgt());
        assertTrue(n12.getRgt() < n1.getRgt());

        assertEquals(5, n1.getDescendantsCount());
        assertEquals(3, n11.getDescendantsCount());
        assertEquals(0, n111.getDescendantsCount());
        assertEquals(0, n112.getDescendantsCount());
        assertEquals(0, n2.getDescendantsCount());
        assertEquals(0, n12.getDescendantsCount());

        assertEquals(0, n1.getFoldedUnderId());
        assertEquals(0, n11.getFoldedUnderId());
        assertEquals(shelf.getNote("Note 1.1").getId(), n111.getFoldedUnderId());
        assertEquals(shelf.getNote("Note 1.1").getId(), n112.getFoldedUnderId());
        assertEquals(shelf.getNote("Note 1.1").getId(), n2.getFoldedUnderId());
        assertEquals(0, n12.getFoldedUnderId());
    }

    @Test
    public void testCutNoteUnderFoldedThenPaste() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n");

        /* Cut & paste hidden 1.1.1 */
        shelf.toggleFoldedState(shelf.getNote("Note 1.1").getId());
        shelf.cut(book.getId(), shelf.getNote("Note 1.1.1").getId());
        Note n = shelf.getNote("Note 1");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.UNDER);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "** Note 1.1.1\n"
                ,
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n111 = shelf.getNote("Note 1.1.1").getPosition();

        assertEquals(0, n1.getFoldedUnderId());
        assertEquals(0, n11.getFoldedUnderId());
        assertEquals(0, n111.getFoldedUnderId());
    }

    @Test
    public void testPromote() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "* Note 2\n");

        Note note = shelf.getNote("Note 1.1");

        /* TODO: Assert 0 if trying to promote level 1 note. */
        assertEquals(1, shelf.promote(book.getId(), note.getId()));

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "* Note 1.1\n" +
                     "* Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();

        assertEquals(0, n1.getDescendantsCount());
        assertEquals(0, n11.getDescendantsCount());
        assertEquals(0, n2.getDescendantsCount());

        assertEquals(1, n1.getLevel());
        assertEquals(1, n11.getLevel());
        assertEquals(1, n2.getLevel());

        assertTrue(n1.getLft() < n1.getRgt());
        assertTrue(n1.getRgt() < n11.getLft());
        assertTrue(n11.getLft() < n11.getRgt());
        assertTrue(n11.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
    }

    @Test
    public void testPromote2() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "** Note 1.2\n" +
                                                         "* Note 2\n");

        Note note = shelf.getNote("Note 1.1.1");

        /* Promote 1.1.1 twice. */
        assertEquals(1, shelf.promote(book.getId(), note.getId()));
        assertEquals(1, shelf.promote(book.getId(), note.getId()));

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "** Note 1.2\n" +
                     "* Note 1.1.1\n" +
                     "* Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n12 = shelf.getNote("Note 1.2").getPosition();
        NotePosition n111 = shelf.getNote("Note 1.1.1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();

        assertEquals(2, n1.getDescendantsCount());
        assertEquals(0, n11.getDescendantsCount());
        assertEquals(0, n12.getDescendantsCount());
        assertEquals(0, n111.getDescendantsCount());
        assertEquals(0, n2.getDescendantsCount());

        assertEquals(1, n1.getLevel());
        assertEquals(2, n11.getLevel());
        assertEquals(2, n12.getLevel());
        assertEquals(1, n111.getLevel());
        assertEquals(1, n2.getLevel());

        assertTrue(n1.getLft() < n11.getLft());
        assertTrue(n11.getLft() < n11.getRgt());
        assertTrue(n11.getRgt() < n12.getLft());
        assertTrue(n12.getLft() < n12.getRgt());
        assertTrue(n12.getRgt() < n1.getRgt());
        assertTrue(n1.getRgt() < n111.getLft());
        assertTrue(n111.getLft() < n111.getRgt());
        assertTrue(n111.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
    }

    @Test
    public void testPromoteFolded() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "** Note 1.2\n" +
                                                         "* Note 2\n");

        Note note = shelf.getNote("Note 1.1");

        /* Promote folded 1.1 */
        shelf.toggleFoldedState(note.getId());
        assertEquals(1, shelf.promote(book.getId(), note.getId()));

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.2\n" +
                     "* Note 1.1\n" +
                     "** Note 1.1.1\n" +
                     "* Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n12 = shelf.getNote("Note 1.2").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n111 = shelf.getNote("Note 1.1.1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();

        assertEquals(1, n1.getDescendantsCount());
        assertEquals(0, n12.getDescendantsCount());
        assertEquals(1, n11.getDescendantsCount());
        assertEquals(0, n111.getDescendantsCount());
        assertEquals(0, n2.getDescendantsCount());

        assertEquals(1, n1.getLevel());
        assertEquals(2, n12.getLevel());
        assertEquals(1, n11.getLevel());
        assertEquals(2, n111.getLevel());
        assertEquals(1, n2.getLevel());

        assertEquals(0, n1.getFoldedUnderId());
        assertEquals(0, n12.getFoldedUnderId());
        assertEquals(0, n11.getFoldedUnderId());
        assertEquals(shelf.getNote("Note 1.1").getId(), n111.getFoldedUnderId());
        assertEquals(0, n2.getFoldedUnderId());

        assertFalse(n1.isFolded());
        assertFalse(n12.isFolded());
        assertTrue(n11.isFolded());
        assertFalse(n111.isFolded());
        assertFalse(n2.isFolded());

        assertTrue(n1.getLft() < n12.getLft());
        assertTrue(n12.getLft() < n12.getRgt());
        assertTrue(n12.getRgt() < n11.getRgt());
        assertTrue(n1.getRgt() < n11.getLft());
        assertTrue(n11.getLft() < n111.getLft());
        assertTrue(n111.getLft() < n111.getRgt());
        assertTrue(n111.getRgt() < n11.getRgt());
        assertTrue(n11.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
    }

    @Test
    public void testDemote() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "* Note 2\n");

        /* Demote 2. */
        assertEquals(1, shelf.demote(book.getId(), shelf.getNote("Note 2").getId()));

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "** Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();

        assertEquals(2, n1.getDescendantsCount());
        assertEquals(0, n11.getDescendantsCount());
        assertEquals(0, n2.getDescendantsCount());

        assertEquals(1, n1.getLevel());
        assertEquals(2, n11.getLevel());
        assertEquals(2, n2.getLevel());

        assertTrue(n1.getLft() < n11.getLft());
        assertTrue(n11.getLft() < n11.getRgt());
        assertTrue(n11.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());
        assertTrue(n2.getRgt() < n1.getRgt());
    }

    @Test
    public void testNewBelowFoldable() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n");

        Note newNote = new Note();
        newNote.getPosition().setBookId(book.getId());
        newNote.getHead().setTitle("Note 2");

        shelf.createNote(
                newNote,
                new NotePlace(book.getId(), shelf.getNote("Note 1").getId(), Place.BELOW)
        );

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "* Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        NotePosition n1 = shelf.getNote("Note 1").getPosition();
        NotePosition n11 = shelf.getNote("Note 1.1").getPosition();
        NotePosition n2 = shelf.getNote("Note 2").getPosition();

        assertTrue(n1.getLft() < n11.getLft());
        assertTrue(n11.getLft() < n11.getRgt());
        assertTrue(n11.getRgt() < n1.getRgt());
        assertTrue(n1.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());

        assertEquals(0, n2.getDescendantsCount());
        assertEquals(shelf.getNote("").getId(), n2.getParentId());
    }

    @Test
    public void testPasteFolded() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "* Note 2\n");

        shelf.toggleFoldedState(shelf.getNote("Note 1").getId());
        shelf.cut(book.getId(), shelf.getNote("Note 1").getId());
        Note n = shelf.getNote("Note 2");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.ABOVE);

        /* Remains folded. */
        assertTrue(shelf.getNote("Note 1").getPosition().isFolded());
        assertNotSame(0, shelf.getNote("Note 1.1").getPosition().getFoldedUnderId());
    }

    @Test
    public void testPasteUnderHidden() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "* Note 2\n");

        shelf.toggleFoldedState(shelf.getNote("Note 1").getId());
        shelf.cut(book.getId(), shelf.getNote("Note 2").getId());
        Note n = shelf.getNote("Note 1.1");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.UNDER);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "*** Note 2\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));

        assertTrue(shelf.getNote("Note 1").getPosition().isFolded());
        assertEquals(shelf.getNote("Note 1").getId(), shelf.getNote("Note 1.1").getPosition().getFoldedUnderId());
        assertEquals(shelf.getNote("Note 1").getId(), shelf.getNote("Note 2").getPosition().getFoldedUnderId());
    }

    @Test
    public void testDemoteNoChanges() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "* Note 2\n" +
                                                         "** Note 2.1\n" +
                                                         "*** Note 2.1.1\n");

        /* Demote 2.1. */
        assertEquals(0, shelf.demote(book.getId(), shelf.getNote("Note 2.1").getId()));

        assertEquals("description\n" +
                     "\n" +
                     "* Note 1\n" +
                     "** Note 1.1\n" +
                     "*** Note 1.1.1\n" +
                     "* Note 2\n" +
                     "** Note 2.1\n" +
                     "*** Note 2.1.1\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));
    }


    @Test
    public void testNewNoteUnder() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "description\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "** Note 1.2\n");

        NotePosition note1, note11, note111, note12, note112;

        note1 = shelf.getNote("Note 1").getPosition();
        note11 = shelf.getNote("Note 1.1").getPosition();
        note111 = shelf.getNote("Note 1.1.1").getPosition();
        note12 = shelf.getNote("Note 1.2").getPosition();

        assertTrue(note1.getLft() < note11.getLft());
        assertTrue(note11.getLft() < note111.getLft());
        assertTrue(note111.getLft() < note111.getRgt());
        assertTrue(note111.getRgt() < note11.getRgt());
        assertTrue(note11.getRgt() < note12.getLft());
        assertTrue(note12.getLft() < note12.getRgt());
        assertTrue(note12.getRgt() < note1.getRgt());

        assertEquals(3, note1.getDescendantsCount());
        assertEquals(1, note11.getDescendantsCount());
        assertEquals(0, note111.getDescendantsCount());
        assertEquals(0, note12.getDescendantsCount());

        /* Create new note under Note 1.1. */
        Note n = new Note();
        n.getPosition().setBookId(book.getId());
        n.getHead().setTitle("Note 1.1.2");
        NotePlace target = new NotePlace(book.getId(), shelf.getNote("Note 1.1").getId(), Place.UNDER);
        shelf.createNote(n, target);


        note1 = shelf.getNote("Note 1").getPosition();
        note11 = shelf.getNote("Note 1.1").getPosition();
        note111 = shelf.getNote("Note 1.1.1").getPosition();
        note112 = shelf.getNote("Note 1.1.2").getPosition();
        note12 = shelf.getNote("Note 1.2").getPosition();

        assertTrue(note1.getLft() < note11.getLft());
        assertTrue(note11.getLft() < note111.getLft());
        assertTrue(note111.getLft() < note111.getRgt());
        assertTrue(note111.getRgt() < note112.getLft());
        assertTrue(note112.getLft() < note112.getRgt());
        assertTrue(note112.getRgt() < note11.getRgt());
        assertTrue(note11.getRgt() < note12.getLft());
        assertTrue(note12.getLft() < note12.getRgt());
        assertTrue(note12.getRgt() < note1.getRgt());

        assertEquals(4, note1.getDescendantsCount());
        assertEquals(2, note11.getDescendantsCount());
        assertEquals(0, note111.getDescendantsCount());
        assertEquals(0, note112.getDescendantsCount());
        assertEquals(0, note12.getDescendantsCount());
    }

    @Test
    public void testNewNoteAbove() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "description\n" +
                                                         "* Note 1\n" +
                                                         // ** Note 1.0
                                                         "** Note 1.1\n" +
                                                         "*** Note 1.1.1\n" +
                                                         "** Note 1.2\n");

        NotePosition note1, note11, note111, note12, note10;

        /* Create new note above Note 1.1. */
        Note n = new Note();
        n.getPosition().setBookId(book.getId());
        n.getHead().setTitle("Note 1.0");
        NotePlace target = new NotePlace(book.getId(), shelf.getNote("Note 1.1").getId(), Place.ABOVE);
        shelf.createNote(n, target);

        note1 = shelf.getNote("Note 1").getPosition();
        note10 = shelf.getNote("Note 1.0").getPosition();
        note11 = shelf.getNote("Note 1.1").getPosition();
        note111 = shelf.getNote("Note 1.1.1").getPosition();
        note12 = shelf.getNote("Note 1.2").getPosition();

        assertTrue(note1.getLft() < note10.getLft());
        assertTrue(note10.getLft() < note10.getRgt());
        assertTrue(note10.getRgt() < note11.getLft());
        assertTrue(note11.getLft() < note111.getLft());
        assertTrue(note111.getLft() < note111.getRgt());
        assertTrue(note111.getRgt() < note11.getRgt());
        assertTrue(note11.getRgt() < note12.getLft());
        assertTrue(note12.getLft() < note12.getRgt());
        assertTrue(note12.getRgt() < note1.getRgt());
    }

    @Test
    public void testCyclingFreshlyImportedNotebook() {
        Book book = shelfTestUtils.setupBook("booky", "Notebook\n" +
                                                      "* Note 1\n" +
                                                      "** Note 2\n" +
                                                      "*** Note 3\n" +
                                                      "** Note 4\n" +
                                                      "* Note 5");

        /* Fold all. */
        shelf.cycleVisibility(book);

        assertTrue(shelf.getNote("Note 1").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 2").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 3").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 4").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 5").getPosition().isFolded());

        /* Unfold all. */
        shelf.cycleVisibility(book);

        assertFalse(shelf.getNote("Note 1").getPosition().isFolded());
        assertFalse(shelf.getNote("Note 2").getPosition().isFolded());
        assertFalse(shelf.getNote("Note 3").getPosition().isFolded());
        assertFalse(shelf.getNote("Note 4").getPosition().isFolded());
        assertFalse(shelf.getNote("Note 5").getPosition().isFolded());
    }

    @Test
    public void testCyclingFoldedState() {
        Book book = shelfTestUtils.setupBook("booky", "" +
                                                      "Notebook\n" +
                                                      "* Note 1\n" +
                                                      "** Note 2\n" +
                                                      "*** Note 3\n" +
                                                      "** Note 4\n" +
                                                      "* Note 5\n" +
                                                      "** Note 6");

        /* Fold all. */
        shelf.cycleVisibility(book);

        /* Unfold Note 1. */
        shelf.toggleFoldedState(shelf.getNote("Note 1").getId());

        assertEquals(0, shelf.getNote("Note 2").getPosition().getFoldedUnderId());
        assertEquals(0, shelf.getNote("Note 4").getPosition().getFoldedUnderId());

        /* Fold all. */
        shelf.cycleVisibility(book);

        /* Unfold all. */
        shelf.cycleVisibility(book);

        /* Fold Note 1. */
        shelf.toggleFoldedState(shelf.getNote("Note 1").getId());

        /* Fold all. */
        shelf.cycleVisibility(book);

        /* Unfold Note 1. */
        shelf.toggleFoldedState(shelf.getNote("Note 1").getId());

        assertFalse(shelf.getNote("Note 1").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 2").getPosition().isFolded());
    }

    @Test
    public void testCutChildCutParentThenPaste() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n" +
                                                         "** Note 1.1\n" +
                                                         "* Note 2\n");

        shelf.cut(book.getId(), shelf.getNote("Note 1.1").getId());
        shelf.cut(book.getId(), shelf.getNote("Note 1").getId());
        Note n = shelf.getNote("Note 2");
        shelf.paste(n.getPosition().getBookId(), n.getId(), Place.UNDER);

        assertEquals("description\n" +
                     "\n" +
                     "* Note 2\n" +
                     "** Note 1\n",
                shelf.getBookContent("notebook", BookName.Format.ORG));
    }

    @Test
    public void testParentIds() throws IOException {
        shelfTestUtils.setupBook("notebook", "description\n" +
                                             "* Note 1\n" +
                                             "** Note 1.1\n" +
                                             "*** Note 1.1.1\n" +
                                             "** Note 1.2\n");

        assertEquals(0, shelf.getNote("").getPosition().getParentId());
        assertEquals(shelf.getNote("").getId(), shelf.getNote("Note 1").getPosition().getParentId());
        assertEquals(shelf.getNote("Note 1").getId(), shelf.getNote("Note 1.1").getPosition().getParentId());
        assertEquals(shelf.getNote("Note 1.1").getId(), shelf.getNote("Note 1.1.1").getPosition().getParentId());
        assertEquals(shelf.getNote("Note 1").getId(), shelf.getNote("Note 1.2").getPosition().getParentId());
    }

    @Test
    public void testParentIdForCreatedNote() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "\n" +
                                                         "* Note 1\n");

        Note newNote = new Note();
        newNote.getPosition().setBookId(book.getId());
        newNote.getHead().setTitle("Note 1.1");

        shelf.createNote(
                newNote,
                new NotePlace(book.getId(), shelf.getNote("Note 1").getId(), Place.UNDER)
        );

        assertEquals(1, shelf.getNote("Note 1").getPosition().getDescendantsCount());
        assertEquals(shelf.getNote("Note 1").getId(), shelf.getNote("Note 1.1").getPosition().getParentId());
    }

    @Test
    public void testFoldingAllWhenContentOnlyIsFolded() {
        Book book = shelfTestUtils.setupBook("notebook", "" +
                                                         "description\n" +
                                                         "* Note 1\n" +
                                                         "** Note 2\n" +
                                                         "* Note 3\n" +
                                                         "Content");

        /* Fold all. */
        shelf.cycleVisibility(book);

        /* Unfold Note 3's content. */
        shelf.toggleFoldedState(shelf.getNote("Note 3").getId());

        /* Fold all. */
        shelf.cycleVisibility(book);

        assertTrue(shelf.getNote("Note 1").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 2").getPosition().isFolded());
        assertTrue(shelf.getNote("Note 3").getPosition().isFolded());
    }

    @Test
    public void testInheritedTagsAfterCutAndPaste() {
        Book book = shelfTestUtils.setupBook("notebook",
                "* A :a:\n" +
                "** B :b:\n" +
                "*** C :c:\n" +
                "* D :d:\n");

        shelf.cut(book.getId(), shelf.getNote("B").getId());
        shelf.paste(book.getId(), shelf.getNote("D").getId(), Place.UNDER);

        assertEquals(1, shelf.getNoteWithExtras("B").getInheritedTags().size());
        assertEquals(2, shelf.getNoteWithExtras("C").getInheritedTags().size());
    }

    /* Make sure root node's rgt is larger then notes'. */
    @Test
    public void testCutAndPaste() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "* Note 1\n* Note 2");
        shelf.cut(book.getId(), shelf.getNote("Note 1").getId());
        shelf.paste(book.getId(), shelf.getNote("Note 2").getId(), Place.BELOW);

        // Compare to root note
        assertTrue(shelf.getNote("").getPosition().getRgt() > shelf.getNote("Note 1").getPosition().getRgt());
        assertTrue(shelf.getNote("").getPosition().getRgt() > shelf.getNote("Note 2").getPosition().getRgt());
    }

    /* After moving one note under another, test lft and rgt od third newly created note. */
    @Test
    public void testNewNoteAfterMovingUnder() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "* Note 1\n* Note 2");
        shelf.cut(book.getId(), shelf.getNote("Note 1").getId());
        shelf.paste(book.getId(), shelf.getNote("Note 2").getId(), Place.UNDER);

        Note newNote = new Note();
        newNote.getPosition().setBookId(book.getId());
        newNote.getHead().setTitle("Note 3");

        shelf.createNote(newNote, null);

        assertTrue(shelf.getNote("Note 2").getPosition().getRgt() < shelf.getNote("Note 3").getPosition().getLft());
    }

    @Test
    public void testMoveNoteDown() throws IOException {
        Book book = shelfTestUtils.setupBook(
                "test_book",
                "* TODO First\n" +
                        "SCHEDULED: <2018-04-24 Tue>\n" +
                        "\n" +
                        "content\n" +
                        "\n" +
                        "** 1.1\n" +
                        "** 1.2\n" +
                        "\n" +
                        "* TODO Second\n" +
                        "SCHEDULED: <2018-04-23 Mon>\n" +
                        "\n" +
                        "** 2.1\n" +
                        "** 2.2\n" +
                        "\n" +
                        "* TODO Third\n"

        );

        Note firstNote = shelf.getNote("First");

        shelf.move(book.getId(), firstNote.getId(), 1);

        String actual = shelf.getBookContent("test_book", BookName.Format.ORG);

        String expectedBook = "* TODO Second\n" +
                "SCHEDULED: <2018-04-23 Mon>\n" +
                "\n" +
                "** 2.1\n" +
                "** 2.2\n" +
                "* TODO First\n" +
                "SCHEDULED: <2018-04-24 Tue>\n" +
                "\n" +
                "content\n" +
                "\n" +
                "** 1.1\n" +
                "** 1.2\n" +
                "* TODO Third\n";

        assertEquals(expectedBook, actual);
    }

    @Test
    public void testRefile() throws IOException {
        Book book = shelfTestUtils.setupBook(
                "Source",
                "* TODO First\n" +
                        "SCHEDULED: <2018-04-24 Tue>\n" +
                        "\n" +
                        "content\n" +
                        "\n" +
                        "** 1.1\n" +
                        "** 1.2\n" +
                        "\n" +
                        "* TODO RefileMe\n" +
                        "SCHEDULED: <2018-04-23 Mon>\n" +
                        "\n" +
                        "** 2.1\n" +
                        "** 2.2\n" +
                        "\n" +
                        "* TODO Third\n"

        );

        Book targetBook = shelfTestUtils.setupBook(
                "REFILE",
                "* TODO RefiledPreviously\n"
                );

        Note refileNote = shelf.getNote("RefileMe");

        shelf.refile(book.getId(), Collections.singleton(refileNote.getId()), targetBook.getId());

        String actual = shelf.getBookContent("REFILE", BookName.Format.ORG);

        String expectedBook = "* TODO RefiledPreviously\n" +
                "* TODO RefileMe\n" +
                "SCHEDULED: <2018-04-23 Mon>\n" +
                "\n" +
                "** 2.1\n" +
                "** 2.2\n";

        assertEquals(expectedBook, actual);
    }
}
