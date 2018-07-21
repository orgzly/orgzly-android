package com.orgzly.android.repos;

import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.Note;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.sync.BookNamesake;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ShelfTest extends OrgzlyTest {
    private static final String TAG = ShelfTest.class.getName();

    @Test
    public void testNewBook() throws IOException {
        shelf.createBook("booky");

        /* Make sure root node is created. */
        Note note = shelf.getNote("");
        assertEquals(1, note.getPosition().getLft());
        assertEquals(2, note.getPosition().getRgt());
        assertEquals(0, note.getPosition().getLevel());
        assertNotNull(note.getHead());
        assertEquals("", note.getHead().getTitle());
    }

    @Test
    public void testInsertDeletedRepo() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.deleteRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-a");
    }

    @Test
    public void testRepoAndShelfSetup() throws IOException {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-2.org", "", "1abcdef", 1300067156000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-3.org", "", "2abcdef", 1200067156000L);

        shelfTestUtils.setupBook("local-book-1", "");

        assertEquals("Local books", 1, shelf.getBooks().size());
        assertEquals("Remote books", 3, shelf.getBooksFromAllRepos(null).size());
    }

    @Test
    public void testLoadRook() throws IOException {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-2.org", "", "1abcdef", 1300067156000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-3.org", "", "2abcdef", 1200067156000L);

        VersionedRook vrook = shelf.getBooksFromAllRepos(null).get(0);

        shelf.loadBookFromRepo(vrook);

        assertEquals(1, shelf.getBooks().size());
        Book book = shelf.getBooks().get(0);

        assertEquals("remote-book-1", book.getName());
        assertEquals("/remote-book-1.org", book.getLastSyncedToRook().getUri().getPath());
        assertEquals("remote-book-1", BookName.getInstance(context, book.getLastSyncedToRook()).getName());
        assertEquals("0abcdef", book.getLastSyncedToRook().getRevision());
        assertEquals(1400067156000L, book.getLastSyncedToRook().getMtime());
    }

    @Test
    public void testCompareWithEmptyRepo() throws IOException {
        assertEquals("Starting with empty shelf", 0, shelf.getBooks().size());

        Map<String, BookNamesake> nameGroups = shelf.groupAllNotebooksByName();

        assertEquals(0, nameGroups.size());
    }

    @Test
    public void testCompareWithRepo() throws IOException {
        assertEquals("Starting with empty shelf", 0, shelf.getBooks().size());
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-2.org", "", "1abcdef", 1400412756);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-3.org", "", "2abcdef", 1400671956);

        Map<String, BookNamesake> groups = shelf.groupAllNotebooksByName();

        assertEquals(3, groups.size());

        for (BookNamesake group : groups.values()) {
            String name = group.getName();

            assertTrue("Book name " + name + " not expected",
                    name.equals("remote-book-1") || name.equals("remote-book-2") || name.equals("remote-book-3"));

            assertTrue(group.getBook().isDummy());
            assertEquals(1, group.getRooks().size());
        }
    }

    @Test
    public void testShelfAndRepo() throws IOException {
        assertEquals("Starting with empty shelf", 0, shelf.getBooks().size());

        Book book;

        book = shelf.createBook("local-book-1");
        assertEquals("local-book-1", book.getName());
        assertNull(book.getLastSyncedToRook());

        book = shelf.createBook("common-book-1");
        assertEquals("common-book-1", book.getName());
        assertNull(book.getLastSyncedToRook());

        book = shelf.createBook("common-book-2");
        assertEquals("common-book-2", book.getName());
        assertNull(book.getLastSyncedToRook());

        /* Setup mock repo. */
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-2.org", "", "1abcdef", 1400412756000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/common-book-1.org", "", "2abcdef", 1400671956000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/common-book-2.org", "", "3abcdef", 1400671956000L);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156000L);

        Map<String, BookNamesake> groups = shelf.groupAllNotebooksByName();

        assertEquals(5, groups.size());

        for (BookNamesake group : groups.values()) {
            String name = group.getName();

            if (name.equals("local-book-1")) {
                assertFalse(group.getBook().isDummy());
                assertEquals(0, group.getRooks().size());

            } else if (name.equals("common-book-1")) {
                assertFalse(group.getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else if (name.equals("common-book-2")) {
                assertFalse(group.getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else if (name.equals("remote-book-1")) {
                assertTrue(group.getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else if (name.equals("remote-book-2")) {
                assertTrue(group.getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else {
                fail("unexpected name " + name);
            }
        }
    }
}
