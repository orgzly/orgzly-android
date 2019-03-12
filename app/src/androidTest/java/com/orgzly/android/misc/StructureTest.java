package com.orgzly.android.misc;

import com.orgzly.android.BookFormat;
import com.orgzly.android.NotesClipboard;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.Note;
import com.orgzly.android.db.entity.NotePosition;
import com.orgzly.android.db.entity.NoteView;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.android.ui.note.NotePayload;
import com.orgzly.android.usecase.BookCycleVisibility;
import com.orgzly.android.usecase.NoteCreate;
import com.orgzly.android.usecase.NoteCut;
import com.orgzly.android.usecase.NoteDelete;
import com.orgzly.android.usecase.NotePaste;
import com.orgzly.android.usecase.NoteToggleFolding;
import com.orgzly.android.usecase.UseCaseResult;
import com.orgzly.android.usecase.UseCaseRunner;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StructureTest extends OrgzlyTest {
    @Test
    public void testNewNote() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n");

        UseCaseRunner.run(new NoteCreate(
                new NotePayload("Note 2"),
                new NotePlace(book.getBook().getId())));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        assertEquals(
                dataRepository.getRootNode(book.getBook().getId()).getId(),
                dataRepository.getNote("Note 2").getPosition().getParentId());

    }

    @Test
    public void testBookSetupLevels() {
        BookView bookView = testUtils.setupBook("notebook", "" +
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

        assertEquals(0, dataRepository.getRootNode(bookView.getBook().getId()).getPosition().getLevel());
        assertEquals(1, dataRepository.getNote("Note #1.").getPosition().getLevel());
        assertEquals(1, dataRepository.getNote("Note #2.").getPosition().getLevel());
        assertEquals(2, dataRepository.getNote("Note #3.").getPosition().getLevel());
        assertEquals(2, dataRepository.getNote("Note #4.").getPosition().getLevel());
        assertEquals(3, dataRepository.getNote("Note #5.").getPosition().getLevel());
        assertEquals(4, dataRepository.getNote("Note #6.").getPosition().getLevel());
        assertEquals(2, dataRepository.getNote("Note #7.").getPosition().getLevel());
        assertEquals(1, dataRepository.getNote("Note #8.").getPosition().getLevel());
        assertEquals(4, dataRepository.getNote("Note #9.").getPosition().getLevel());
        assertEquals(2, dataRepository.getNote("Note #10.").getPosition().getLevel());
    }

    @Test
    public void testCut() {
        BookView book = testUtils.setupBook("notebook", "" +
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
        ids.add(dataRepository.getNote("Note #1.").getId());
        ids.add(dataRepository.getNote("Note #3.").getId());

        UseCaseRunner.run(new NoteCut(book.getBook().getId(), ids));

        assertEquals(8, dataRepository.getNoteCount(book.getBook().getId()));

        List<NoteView> notes = dataRepository.getNotes(book.getBook().getName());

        assertEquals("Title for book should match", "Note #2.", notes.get(0).getNote().getTitle());
        assertEquals("Level for book should match", 1, notes.get(0).getNote().getPosition().getLevel());

        assertEquals("Title for book should match", "Note #4.", notes.get(1).getNote().getTitle());
        assertEquals("Level for book should match", 2, notes.get(1).getNote().getPosition().getLevel());

        assertEquals("Title for book should match", "Note #5.", notes.get(2).getNote().getTitle());
        assertEquals("Level for book should match", 3, notes.get(2).getNote().getPosition().getLevel());
    }

    @Test
    public void testPasteToDifferentBook() throws IOException {
        testUtils.setupBook(
                "notebook-1", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n");

        BookView book2 = testUtils.setupBook(
                "notebook-2", "" +
                "description\n" +
                "\n" +
                "* Note A\n" +
                "** Note A.A\n" +
                "*** Note A.A.A\n");


        UseCaseRunner.run(new NoteCut(
                book2.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note A.A").getId())));

        Note n = dataRepository.getNote("Note 1.1.1");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.UNDER));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "*** Note 1.1.1\n" +
                        "**** Note A.A\n" +
                        "***** Note A.A.A\n",
                dataRepository.getBookContent("notebook-1", BookFormat.ORG));

        assertEquals("description\n" +
                        "\n" +
                        "* Note A\n",
                dataRepository.getBookContent("notebook-2", BookFormat.ORG));
    }

    @Test
    public void testDescendantCountAfterCut() {
        BookView book = testUtils.setupBook("notebook", "* Note 1\n** Note 1.1\n");
        assertEquals(1, dataRepository.getNote("Note 1").getPosition().getDescendantsCount());
        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1.1").getId())));
        assertEquals(0, dataRepository.getNote("Note 1").getPosition().getDescendantsCount());
    }

    @Test

    public void testDescendantCountAfterDelete() {
        BookView book = testUtils.setupBook("notebook", "* Note 1\n** Note 1.1\n");
        assertEquals(1, dataRepository.getNote("Note 1").getPosition().getDescendantsCount());
        UseCaseRunner.run(new NoteDelete(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1.1").getId())));
        assertEquals(0, dataRepository.getNote("Note 1").getPosition().getDescendantsCount());
    }

    @Test
    public void testPasteUnder() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "** Note 1.2\n" +
                "*** Note 1.2.1\n" +
                "*** Note 1.2.2\n" +
                "* Note 2\n");

        /* Cut 1.1 and paste it under 1.2. */
        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1.1").getId())));
        Note n = dataRepository.getNote("Note 1.2");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.UNDER));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.2\n" +
                        "*** Note 1.2.1\n" +
                        "*** Note 1.2.2\n" +
                        "*** Note 1.1\n" +
                        "**** Note 1.1.1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n12 = dataRepository.getNote("Note 1.2").getPosition();
        NotePosition n121 = dataRepository.getNote("Note 1.2.1").getPosition();
        NotePosition n122 = dataRepository.getNote("Note 1.2.2").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n111 = dataRepository.getNote("Note 1.1.1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

        assertEquals(dataRepository.getNote("Note 1.2").getId(), n11.getParentId());

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
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "*** Note 1.1.2\n" +
                "** Note 1.2\n" +
                "* Note 2\n");

        /* Cut & paste 2 under folded 1.1. */
        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 2").getId())));
        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1.1").getId()));
        Note n = dataRepository.getNote("Note 1.1");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.UNDER));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "*** Note 1.1.1\n" +
                        "*** Note 1.1.2\n" +
                        "*** Note 2\n" +
                        "** Note 1.2\n"
                ,
                dataRepository.getBookContent("notebook", BookFormat.ORG));


        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n111 = dataRepository.getNote("Note 1.1.1").getPosition();
        NotePosition n112 = dataRepository.getNote("Note 1.1.2").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();
        NotePosition n12 = dataRepository.getNote("Note 1.2").getPosition();

        assertEquals(dataRepository.getNote("Note 1.1").getId(), n2.getParentId());

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
        assertEquals(dataRepository.getNote("Note 1.1").getId(), n111.getFoldedUnderId());
        assertEquals(dataRepository.getNote("Note 1.1").getId(), n112.getFoldedUnderId());
        assertEquals(dataRepository.getNote("Note 1.1").getId(), n2.getFoldedUnderId());
        assertEquals(0, n12.getFoldedUnderId());
    }

    @Test
    public void testCutNoteUnderFoldedThenPaste() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n");

        /* Cut & paste hidden 1.1.1 */
        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1.1").getId()));
        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1.1.1").getId())));
        Note n = dataRepository.getNote("Note 1");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.UNDER));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "** Note 1.1.1\n"
                ,
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n111 = dataRepository.getNote("Note 1.1.1").getPosition();

        assertEquals(0, n1.getFoldedUnderId());
        assertEquals(0, n11.getFoldedUnderId());
        assertEquals(0, n111.getFoldedUnderId());
    }

    @Test
    public void testPromote() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "* Note 2\n");

        Note note = dataRepository.getNote("Note 1.1");

        assertEquals(1, dataRepository.promoteNote(book.getBook().getId(), note.getId()));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "* Note 1.1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

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
    public void testPromoteFirstLevelNote() {
        BookView book = testUtils.setupBook("notebook", "* Note 1");

        Note note = dataRepository.getNote("Note 1");

        assertEquals(0, dataRepository.promoteNote(book.getBook().getId(), note.getId()));
    }

    @Test
    public void testPromote2() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "** Note 1.2\n" +
                "* Note 2\n");

        Note note = dataRepository.getNote("Note 1.1.1");

        /* Promote 1.1.1 twice. */
        assertEquals(1, dataRepository.promoteNote(book.getBook().getId(), note.getId()));
        assertEquals(1, dataRepository.promoteNote(book.getBook().getId(), note.getId()));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "** Note 1.2\n" +
                        "* Note 1.1.1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n12 = dataRepository.getNote("Note 1.2").getPosition();
        NotePosition n111 = dataRepository.getNote("Note 1.1.1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

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
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "** Note 1.2\n" +
                "* Note 2\n");

        Note note = dataRepository.getNote("Note 1.1");

        /* Promote folded 1.1 */
        UseCaseRunner.run(new NoteToggleFolding(note.getId()));
        assertEquals(2, dataRepository.promoteNote(book.getBook().getId(), note.getId()));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.2\n" +
                        "* Note 1.1\n" +
                        "** Note 1.1.1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n12 = dataRepository.getNote("Note 1.2").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n111 = dataRepository.getNote("Note 1.1.1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

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
        assertEquals(dataRepository.getNote("Note 1.1").getId(), n111.getFoldedUnderId());
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
    public void testPromoteFirst2LevelOrphan() throws IOException {
        BookView book = testUtils.setupBook(
                "notebook",
                "** Note 1\n" +
                        "* Note 2\n");

        /* Promote first note. */
        dataRepository.promoteNote(book.getBook().getId(), dataRepository.getNote("Note 1").getId());

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

        assertEquals(1, n1.getLevel());
        assertEquals(1, n2.getLevel());

        assertTrue(n1.getLft() + " < " + n1.getRgt(), n1.getLft() < n1.getRgt());
        assertTrue(n1.getRgt() + " < " + n2.getLft(), n1.getRgt() < n2.getLft());
        assertTrue(n2.getLft() + " < " + n2.getRgt(), n2.getLft() < n2.getRgt());

        assertEquals(
                "* Note 1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));
    }

    @Test
    public void testPromote3LevelOrphan() throws IOException {
        BookView book = testUtils.setupBook(
                "notebook",
                "* Note 1\n" +
                        "*** Note 2\n");

        /* Promote first note. */
        dataRepository.promoteNote(book.getBook().getId(), dataRepository.getNote("Note 2").getId());

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

        assertEquals(1, n1.getLevel());
        assertEquals(2, n2.getLevel());

        assertTrue(n1.getLft() + " < " + n2.getLft(), n1.getLft() < n2.getLft());
        assertTrue(n2.getLft() + " < " + n2.getRgt(), n2.getLft() < n2.getRgt());
        assertTrue(n2.getRgt() + " < " + n1.getRgt(), n2.getRgt() < n1.getRgt());

        assertEquals(
                "* Note 1\n" +
                        "** Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));
    }

    @Test
    public void testPromote4LevelOrphan() throws IOException {
        BookView book = testUtils.setupBook(
                "notebook",
                "* Note 1\n" +
                        "**** Note 2\n");

        /* Promote first note. */
        dataRepository.promoteNote(book.getBook().getId(), dataRepository.getNote("Note 2").getId());

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

        assertEquals(1, n1.getLevel());
        assertEquals(2, n2.getLevel());

        assertTrue(n1.getLft() + " < " + n2.getLft(), n1.getLft() < n2.getLft());
        assertTrue(n2.getLft() + " < " + n2.getRgt(), n2.getLft() < n2.getRgt());
        assertTrue(n2.getRgt() + " < " + n1.getRgt(), n2.getRgt() < n1.getRgt());

        assertEquals(
                "* Note 1\n" +
                        "** Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));
    }

    @Test
    public void testDelete() throws IOException {
        BookView book = testUtils.setupBook(
                "notebook",
                "* Note 1\n" +
                        "** Note 1.1\n" +
                        "*** Note 1.1.1\n");

        dataRepository.deleteNotes(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1.1").getId()));

        assertEquals("* Note 1\n", dataRepository.getBookContent("notebook", BookFormat.ORG));
    }

    @Test
    public void testDemote() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "* Note 2\n");

        /* Demote 2. */
        assertEquals(1, dataRepository.demoteNote(book.getBook().getId(), dataRepository.getNote("Note 2").getId()));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "** Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

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
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n");

        UseCaseRunner.run(new NoteCreate(
                new NotePayload("Note 2"),
                new NotePlace(book.getBook().getId(), dataRepository.getNote("Note 1").getId(), Place.BELOW)));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "* Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        NotePosition n1 = dataRepository.getNote("Note 1").getPosition();
        NotePosition n11 = dataRepository.getNote("Note 1.1").getPosition();
        NotePosition n2 = dataRepository.getNote("Note 2").getPosition();

        assertTrue(n1.getLft() < n11.getLft());
        assertTrue(n11.getLft() < n11.getRgt());
        assertTrue(n11.getRgt() < n1.getRgt());
        assertTrue(n1.getRgt() < n2.getLft());
        assertTrue(n2.getLft() < n2.getRgt());

        assertEquals(0, n2.getDescendantsCount());
        assertEquals(dataRepository.getRootNode(book.getBook().getId()).getId(), n2.getParentId());
    }

    @Ignore
    @Test
    public void testPasteFolded() {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "* Note 2\n");

        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1").getId()));

        assertTrue(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertEquals(
                dataRepository.getNote("Note 1").getId(),
                dataRepository.getNote("Note 1.1").getPosition().getFoldedUnderId());

        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1").getId())));

        Note n = dataRepository.getNote("Note 2");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.ABOVE));

        /* Remains folded. */
        assertTrue(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertEquals(
                dataRepository.getNote("Note 1").getId(),
                dataRepository.getNote("Note 1.1").getPosition().getFoldedUnderId());

    }

    @Test
    public void testPasteUnderHidden() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "* Note 2\n");

        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1").getId()));
        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 2").getId())));
        Note n = dataRepository.getNote("Note 1.1");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.UNDER));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "*** Note 2\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));

        assertTrue(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertEquals(dataRepository.getNote("Note 1").getId(), dataRepository.getNote("Note 1.1").getPosition().getFoldedUnderId());
        assertEquals(dataRepository.getNote("Note 1").getId(), dataRepository.getNote("Note 2").getPosition().getFoldedUnderId());
    }

    @Test
    public void testDemoteNoChanges() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "* Note 2\n" +
                "** Note 2.1\n" +
                "*** Note 2.1.1\n");

        /* Demote 2.1. */
        assertEquals(0, dataRepository.demoteNote(book.getBook().getId(), dataRepository.getNote("Note 2.1").getId()));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 1\n" +
                        "** Note 1.1\n" +
                        "*** Note 1.1.1\n" +
                        "* Note 2\n" +
                        "** Note 2.1\n" +
                        "*** Note 2.1.1\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));
    }


    @Test
    public void testNewNoteUnder() throws IOException {
        BookView book = testUtils.setupBook("notebook", "description\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "** Note 1.2\n");

        NotePosition note1, note11, note111, note12, note112;

        note1 = dataRepository.getNote("Note 1").getPosition();
        note11 = dataRepository.getNote("Note 1.1").getPosition();
        note111 = dataRepository.getNote("Note 1.1.1").getPosition();
        note12 = dataRepository.getNote("Note 1.2").getPosition();

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
        UseCaseRunner.run(new NoteCreate(
                new NotePayload("Note 1.1.2"),
                new NotePlace(book.getBook().getId(), dataRepository.getNote("Note 1.1").getId(), Place.UNDER)));


        note1 = dataRepository.getNote("Note 1").getPosition();
        note11 = dataRepository.getNote("Note 1.1").getPosition();
        note111 = dataRepository.getNote("Note 1.1.1").getPosition();
        note112 = dataRepository.getNote("Note 1.1.2").getPosition();
        note12 = dataRepository.getNote("Note 1.2").getPosition();

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
        BookView book = testUtils.setupBook("notebook", "description\n" +
                "* Note 1\n" +
                // ** Note 1.0
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "** Note 1.2\n");

        NotePosition note1, note11, note111, note12, note10;

        /* Create new note above Note 1.1. */
        UseCaseRunner.run(new NoteCreate(
                new NotePayload("Note 1.0"),
                new NotePlace(book.getBook().getId(), dataRepository.getNote("Note 1.1").getId(), Place.ABOVE)));

        note1 = dataRepository.getNote("Note 1").getPosition();
        note10 = dataRepository.getNote("Note 1.0").getPosition();
        note11 = dataRepository.getNote("Note 1.1").getPosition();
        note111 = dataRepository.getNote("Note 1.1.1").getPosition();
        note12 = dataRepository.getNote("Note 1.2").getPosition();

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
        BookView book = testUtils.setupBook("booky", "Notebook\n" +
                "* Note 1\n" +
                "** Note 2\n" +
                "*** Note 3\n" +
                "** Note 4\n" +
                "* Note 5");

        /* Fold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        assertTrue(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 2").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 3").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 4").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 5").getPosition().isFolded());

        /* Unfold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        assertFalse(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertFalse(dataRepository.getNote("Note 2").getPosition().isFolded());
        assertFalse(dataRepository.getNote("Note 3").getPosition().isFolded());
        assertFalse(dataRepository.getNote("Note 4").getPosition().isFolded());
        assertFalse(dataRepository.getNote("Note 5").getPosition().isFolded());
    }

    @Test
    public void testCyclingFoldedState() {
        BookView book = testUtils.setupBook("booky", "" +
                "Notebook\n" +
                "* Note 1\n" +
                "** Note 2\n" +
                "*** Note 3\n" +
                "** Note 4\n" +
                "* Note 5\n" +
                "** Note 6");

        /* Fold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        /* Unfold Note 1. */
        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1").getId()));

        assertEquals(0, dataRepository.getNote("Note 2").getPosition().getFoldedUnderId());
        assertEquals(0, dataRepository.getNote("Note 4").getPosition().getFoldedUnderId());

        /* Fold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        /* Unfold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        /* Fold Note 1. */
        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1").getId()));

        /* Fold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        /* Unfold Note 1. */
        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 1").getId()));

        assertFalse(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 2").getPosition().isFolded());
    }

    @Test
    public void testCutChildCutParentThenPaste() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "* Note 2\n");

        long bookId = book.getBook().getId();

        UseCaseRunner.run(new NoteCut(
                bookId, Collections.singleton(dataRepository.getNote("Note 1.1").getId())));
        UseCaseRunner.run(new NoteCut(
                bookId, Collections.singleton(dataRepository.getNote("Note 1").getId())));

        Note n = dataRepository.getNote("Note 2");
        UseCaseRunner.run(new NotePaste(n.getPosition().getBookId(), n.getId(), Place.UNDER));

        assertEquals("description\n" +
                        "\n" +
                        "* Note 2\n" +
                        "** Note 1\n",
                dataRepository.getBookContent("notebook", BookFormat.ORG));
    }

    @Test
    public void testParentIds() throws IOException {
        BookView bookView = testUtils.setupBook("notebook", "description\n" +
                "* Note 1\n" +
                "** Note 1.1\n" +
                "*** Note 1.1.1\n" +
                "** Note 1.2\n");

        Note rootNode = dataRepository.getRootNode(bookView.getBook().getId());

        assertNotNull(rootNode);
        assertEquals(0, rootNode.getPosition().getParentId());
        assertEquals(rootNode.getId(), dataRepository.getNote("Note 1").getPosition().getParentId());
        assertEquals(dataRepository.getNote("Note 1").getId(), dataRepository.getNote("Note 1.1").getPosition().getParentId());
        assertEquals(dataRepository.getNote("Note 1.1").getId(), dataRepository.getNote("Note 1.1.1").getPosition().getParentId());
        assertEquals(dataRepository.getNote("Note 1").getId(), dataRepository.getNote("Note 1.2").getPosition().getParentId());
    }

    @Test
    public void testParentIdForCreatedNote() throws IOException {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "\n" +
                "* Note 1\n");

        UseCaseRunner.run(new NoteCreate(
                new NotePayload("Note 1.1"),
                new NotePlace(book.getBook().getId(), dataRepository.getNote("Note 1").getId(), Place.UNDER)));

        assertEquals(1, dataRepository.getNote("Note 1").getPosition().getDescendantsCount());
        assertEquals(dataRepository.getNote("Note 1").getId(), dataRepository.getNote("Note 1.1").getPosition().getParentId());
    }

    @Test
    public void testFoldingAllWhenContentOnlyIsFolded() {
        BookView book = testUtils.setupBook("notebook", "" +
                "description\n" +
                "* Note 1\n" +
                "** Note 2\n" +
                "* Note 3\n" +
                "Content");

        /* Fold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        /* Unfold Note 3's content. */
        UseCaseRunner.run(new NoteToggleFolding(dataRepository.getNote("Note 3").getId()));

        /* Fold all. */
        UseCaseRunner.run(new BookCycleVisibility(book.getBook()));

        assertTrue(dataRepository.getNote("Note 1").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 2").getPosition().isFolded());
        assertTrue(dataRepository.getNote("Note 3").getPosition().isFolded());
    }

    @Test
    public void testInheritedTagsAfterCutAndPaste() throws IOException {
        BookView book = testUtils.setupBook(
                "notebook",
                "* A :a:\n" +
                "** B :b:\n" +
                "*** C :c:\n" +
                "* D :d:\n");

        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("B").getId())));

        UseCaseRunner.run(new NotePaste(
                book.getBook().getId(), dataRepository.getNote("D").getId(), Place.UNDER));

        String expectedBook =
                "* A :a:\n" +
                "* D :d:\n" +
                "** B :b:\n" +
                "*** C :c:\n" +
                "";

        assertEquals(expectedBook, dataRepository.getBookContent("notebook", BookFormat.ORG));

        assertEquals(1, dataRepository.getNoteView("B").getInheritedTagsList().size());
        assertEquals(2, dataRepository.getNoteView("C").getInheritedTagsList().size());
    }

    /* Make sure root node's rgt is larger then notes'. */
    @Test
    public void testCutAndPaste() throws IOException {
        BookView bookView = testUtils.setupBook("notebook", "* Note 1\n* Note 2");

        UseCaseRunner.run(new NoteCut(
                bookView.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1").getId())));

        UseCaseRunner.run(new NotePaste(
                bookView.getBook().getId(), dataRepository.getNote("Note 2").getId(), Place.BELOW));

        // Compare to root note
        long bookId = bookView.getBook().getId();
        assertTrue(dataRepository.getRootNode(bookId).getPosition().getRgt() > dataRepository.getNote("Note 1").getPosition().getRgt());
        assertTrue(dataRepository.getRootNode(bookId).getPosition().getRgt() > dataRepository.getNote("Note 2").getPosition().getRgt());
    }

    /* After moving one note under another, test lft and rgt od third newly created note. */
    @Test
    public void testNewNoteAfterMovingUnder() throws IOException {
        BookView book = testUtils.setupBook("notebook", "* Note 1\n* Note 2");

        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note 1").getId())));

        UseCaseRunner.run(new NotePaste(
                book.getBook().getId(), dataRepository.getNote("Note 2").getId(), Place.UNDER));

        UseCaseRunner.run(new NoteCreate(
                new NotePayload("Note 3"), new NotePlace(book.getBook().getId())));

        assertTrue(dataRepository.getNote("Note 2").getPosition().getRgt() < dataRepository.getNote("Note 3").getPosition().getLft());
    }

    @Test
    public void testMoveNoteDown() throws IOException {
        BookView book = testUtils.setupBook(
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

        Note firstNote = dataRepository.getNote("First");

        dataRepository.moveNote(book.getBook().getId(), firstNote.getId(), 1);

        String actual = dataRepository.getBookContent("test_book", BookFormat.ORG);

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
        BookView book = testUtils.setupBook(
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

        BookView targetBook = testUtils.setupBook(
                "REFILE",
                "* TODO RefiledPreviously\n"
        );

        Note refileNote = dataRepository.getNote("RefileMe");

        dataRepository.refileNotes(book.getBook().getId(), Collections.singleton(refileNote.getId()), targetBook.getBook().getId());

        String actual = dataRepository.getBookContent("REFILE", BookFormat.ORG);

        String expectedBook = "* TODO RefiledPreviously\n" +
                "* TODO RefileMe\n" +
                "SCHEDULED: <2018-04-23 Mon>\n" +
                "\n" +
                "** 2.1\n" +
                "** 2.2\n";

        assertEquals(expectedBook, actual);
    }

    @Test
    public void testMultipleNotesCut() {
        BookView book = testUtils.setupBook(
                "Book A",
                "* Note A-01\n" +
                "* Note A-02\n" +
                "** Note A-03\n" +
                "** Note A-04\n" +
                "*** Note A-05\n");

        UseCaseResult result = UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                new HashSet<>(Arrays.asList(
                        dataRepository.getNote("Note A-01").getId(),
                        dataRepository.getNote("Note A-02").getId()
                ))));

        String expectedBook =
                "* Note A-01\n" +
                "* Note A-02\n" +
                "** Note A-03\n" +
                "** Note A-04\n" +
                "*** Note A-05\n" +
                "";

        NotesClipboard clipboard = (NotesClipboard) result.getUserData();

        assertEquals(expectedBook, clipboard.toOrg());
    }

    @Test
    public void testPasteMultipleTimesBelow() throws IOException {
        Note note;

        BookView book = testUtils.setupBook(
                "Book A",
                "* Note A-01\n" +
                "* Note A-02\n");

        UseCaseRunner.run(new NoteCut(
                book.getBook().getId(),
                Collections.singleton(dataRepository.getNote("Note A-02").getId())));

        assertEquals(
                "* Note A-01\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG));

        note = dataRepository.getNote("Note A-01");
        UseCaseRunner.run(new NotePaste(note.getPosition().getBookId(), note.getId(), Place.BELOW));

        assertEquals(
                "* Note A-01\n" +
                "* Note A-02\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG));

        assertTrue(
                dataRepository.getNote("Note A-01").getPosition().getRgt()
                        < dataRepository.getNote("Note A-02").getPosition().getLft());


        note = dataRepository.getNote("Note A-02");
        UseCaseRunner.run(new NotePaste(note.getPosition().getBookId(), note.getId(), Place.BELOW));

        assertEquals(
                "* Note A-01\n" +
                "* Note A-02\n" +
                "* Note A-02\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG));

        note = dataRepository.getNote("Note A-02");
        UseCaseRunner.run(new NotePaste(note.getPosition().getBookId(), note.getId(), Place.BELOW));

        assertEquals(
                "* Note A-01\n" +
                "* Note A-02\n" +
                "* Note A-02\n" +
                "* Note A-02\n",
                dataRepository.getBookContent("Book A", BookFormat.ORG));

        assertTrue(
                dataRepository.getNote("Note A-02").getPosition().getRgt()
                        < dataRepository.getRootNode(book.getBook().getId()).getPosition().getRgt());

    }
}
