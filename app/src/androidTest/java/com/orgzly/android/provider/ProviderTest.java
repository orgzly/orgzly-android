package com.orgzly.android.provider;

import android.database.Cursor;

import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.Note;
import com.orgzly.android.NotePosition;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.repos.VersionedRook;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ProviderTest extends OrgzlyTest {
    /**
     * Used in {@link com.orgzly.android.ui.fragments.BooksFragment} to get all available local books.
     */
    @Test
    public void testQueryBooks() {
        shelfTestUtils.setupBook("book-1", "* Note 1");
        shelfTestUtils.setupBook("book-2", "* Note 1\n** Note 2");

        Cursor cursor = context.getContentResolver().query(ProviderContract.Books.ContentUri.books(), null, null, null, null);

        assertEquals(2, cursor.getCount());

        cursor.close();
    }

    /**
     * Used in {@link com.orgzly.android.ui.fragments.BookFragment} to get specific book.
     */
    @Test
    public void testQueryBook() {
        Book book = shelfTestUtils.setupBook("book-1", "* Note 1\n** Note 2");

        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Books.ContentUri.booksId(book.getId()), null, null, null, null);

        assertEquals(1, cursor.getCount());

        cursor.close();
    }

    /**
     * Used in {@link com.orgzly.android.ui.fragments.BookFragment} to get notes for specific book.
     */
    @Test
    public void testNotesForBook() {
        Book book = shelfTestUtils.setupBook("book-1", "* Note 1\n** Note 2");

        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Books.ContentUri.booksIdNotes(book.getId()), null, null, null, null);

        assertEquals(2, cursor.getCount());

        cursor.close();
    }

    /**
     * Saving book to repo.
     */
    @Test
    public void testSaveBook() throws IOException {
        shelfTestUtils.setupBook("book-1", "* Note 1\n** Note 2");
        shelfTestUtils.setupRepo("mock://repo");

        long now = System.currentTimeMillis();

        Book savedBook = shelf.saveBookToRepo("mock://repo", "book-1.org", shelf.getBook("book-1"), BookName.Format.ORG);
        VersionedRook vrook = savedBook.getLastSyncedToRook();

        assertEquals("book-1", BookName.getInstance(context, vrook).getName());
        assertEquals("mock://repo", vrook.getRepoUri().toString());
        assertEquals("mock://repo/book-1.org", vrook.getUri().toString());
        assertTrue(vrook.getMtime() >= now);

        Book book = shelf.getBook("book-1");
        vrook = book.getLastSyncedToRook();

        assertEquals("book-1", BookName.getInstance(context, vrook).getName());
        assertEquals("mock://repo", vrook.getRepoUri().toString());
        assertEquals("mock://repo/book-1.org", vrook.getUri().toString());
        assertTrue(vrook.getMtime() >= now);
    }

    @Test
    public void testLinkingBook() {
        shelfTestUtils.setupRepo("mock://repo-a");

        Book book = shelfTestUtils.setupBook("book-1", "Local content for book 1");

        shelfTestUtils.setBookLink(book.getId(), "mock://repo-a");
        book = shelf.getBook("book-1");

        assertNotNull(book.getLinkRepo());
        assertNull(book.getLastSyncedToRook());
    }

//      @Test
//      public void testPositionOfFirstAndLastNote() throws IOException {
//          setup.setupBook("book-name", "");
//          Book book = shelf.getBook(1);
//
//          for (int i = 0; i < 10; i++) {
//              NotesHelper.insert(testContext, new Note(new OrgHead(1), book.getId()), null);
//          }
//
//          assertEquals("Number of inserted notes should be 10", 10, NotesHelper.getCount(testContext, book.getId()));
//
//          Note firstNote = NotesHelper.getNote(testContext, 1);
//          Note lastNote = NotesHelper.getNote(testContext, 10);
//
//          assertEquals("Position of first note should be 1", 1, firstNote.getPosition());
//          assertEquals("Position of last note should be 10", 10, lastNote.getPosition());
//      }
//
//      public void testNoBooksFromScratch1() {
//          assertEquals("There should be no books", 0, shelf.getBooks().size());
//      }

    @Test
    public void testNoBooksFromScratch2() {
        assertEquals("There should be no books", 0, shelf.getBooks().size());
    }

    @Test
    public void testCreatingFirstEmptyBook() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "");

        assertEquals("There should be one book", 1, shelf.getBooks().size());
        assertEquals("Book should not contain any notes", 0, NotesClient.getCount(context, book.getId()));
    }

    @Test
    public void testCreatingNotes() throws IOException {
        Book book = shelfTestUtils.setupBook("notebook", "* Note 1");

        NotePosition root, note1, note2;

        root = shelf.getNote("").getPosition();

        assertEquals(1, NotesClient.getCount(context, book.getId()));
        assertNotNull(root);

        Note note = new Note();
        note.getPosition().setBookId(book.getId());
        note.getHead().setTitle("Note 2");
        NotesClient.create(context, note, null, System.currentTimeMillis());

        note1 = shelf.getNote("Note 1").getPosition();
        root = shelf.getNote("").getPosition();
        note2 = shelf.getNote("Note 2").getPosition();

        assertEquals(2, NotesClient.getCount(context, book.getId()));
        assertNotNull(note2);

        assertTrue(root.getLft() < note1.getLft());
        assertTrue(note1.getLft() < note1.getRgt());
        assertTrue(note1.getRgt() < note2.getLft());
        assertTrue(note2.getLft() < note2.getRgt());
        assertTrue(note2.getRgt() < root.getRgt());
    }
}
