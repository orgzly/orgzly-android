package com.orgzly.android.repos;

import com.orgzly.android.BookName;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.Note;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.sync.BookNamesake;
import com.orgzly.android.sync.SyncService;

import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// FIXME: Clean this up - split it up.
public class DataRepositoryTest extends OrgzlyTest {
    private static final String TAG = DataRepositoryTest.class.getName();

    @Test
    public void testRootNodeInNewBook() throws IOException {
        BookView book = dataRepository.createBook("booky");

        Note note = dataRepository.getRootNode(book.getBook().getId());
        assertEquals(1, note.getPosition().getLft());
        assertEquals(2, note.getPosition().getRgt());
        assertEquals(0, note.getPosition().getLevel());
        assertEquals("", note.getTitle());
    }

    @Test
    public void testInsertDeletedRepo() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.deleteRepo("mock://repo-a");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
    }

    @Test
    public void testRepoAndShelfSetup() throws IOException {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156000L);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-2.org", "", "1abcdef", 1300067156000L);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-3.org", "", "2abcdef", 1200067156000L);

        testUtils.setupBook("local-book-1", "");

        assertEquals("Local books", 1, dataRepository.getBooks().size());
        assertEquals("Remote books", 3, SyncService.getBooksFromAllRepos(dataRepository, null).size());
    }

    @Test
    public void testLoadRook() throws IOException {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156000L);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-2.org", "", "1abcdef", 1300067156000L);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-3.org", "", "2abcdef", 1200067156000L);

        VersionedRook vrook = SyncService.getBooksFromAllRepos(dataRepository, null).get(0);

        dataRepository.loadBookFromRepo(vrook);

        assertEquals(1, dataRepository.getBooks().size());
        BookView book = dataRepository.getBooks().get(0);

        assertEquals("remote-book-1", book.getBook().getName());
        assertEquals("/remote-book-1.org", book.getSyncedTo().getUri().getPath());
        assertEquals("remote-book-1", BookName.getInstance(context, book.getSyncedTo()).getName());
        assertEquals("0abcdef", book.getSyncedTo().getRevision());
        assertEquals(1400067156000L, book.getSyncedTo().getMtime());
    }

    @Test
    public void testCompareWithEmptyRepo() throws IOException {
        assertEquals("Starting with empty shelf", 0, dataRepository.getBooks().size());

        Map<String, BookNamesake> nameGroups = SyncService.groupAllNotebooksByName(dataRepository);

        assertEquals(0, nameGroups.size());
    }

    @Test
    public void testCompareWithRepo() throws IOException {
        assertEquals("Starting with empty shelf", 0, dataRepository.getBooks().size());
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-2.org", "", "1abcdef", 1400412756);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-3.org", "", "2abcdef", 1400671956);

        Map<String, BookNamesake> groups = SyncService.groupAllNotebooksByName(dataRepository);

        assertEquals(3, groups.size());

        for (BookNamesake group : groups.values()) {
            String name = group.getName();

            assertTrue("Book name " + name + " not expected",
                    name.equals("remote-book-1") || name.equals("remote-book-2") || name.equals("remote-book-3"));

            assertTrue(group.getBook().getBook().isDummy());
            assertEquals(1, group.getRooks().size());
        }
    }

    @Test
    public void testShelfAndRepo() throws IOException {
        assertEquals("Starting with empty shelf", 0, dataRepository.getBooks().size());

        BookView book;

        book = dataRepository.createBook("local-book-1");
        assertEquals("local-book-1", book.getBook().getName());
        assertNull(book.getSyncedTo());

        book = dataRepository.createBook("common-book-1");
        assertEquals("common-book-1", book.getBook().getName());
        assertNull(book.getSyncedTo());

        book = dataRepository.createBook("common-book-2");
        assertEquals("common-book-2", book.getBook().getName());
        assertNull(book.getSyncedTo());

        /* Setup mock repo. */
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/remote-book-2.org", "", "1abcdef", 1400412756000L);
        testUtils.setupRook(repo, "mock://repo-a/common-book-1.org", "", "2abcdef", 1400671956000L);
        testUtils.setupRook(repo, "mock://repo-a/common-book-2.org", "", "3abcdef", 1400671956000L);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-1.org", "", "0abcdef", 1400067156000L);

        Map<String, BookNamesake> groups = SyncService.groupAllNotebooksByName(dataRepository);

        assertEquals(5, groups.size());

        for (BookNamesake group : groups.values()) {
            String name = group.getName();

            if (name.equals("local-book-1")) {
                assertFalse(group.getBook().getBook().isDummy());
                assertEquals(0, group.getRooks().size());

            } else if (name.equals("common-book-1")) {
                assertFalse(group.getBook().getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else if (name.equals("common-book-2")) {
                assertFalse(group.getBook().getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else if (name.equals("remote-book-1")) {
                assertTrue(group.getBook().getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else if (name.equals("remote-book-2")) {
                assertTrue(group.getBook().getBook().isDummy());
                assertEquals(1, group.getRooks().size());

            } else {
                fail("unexpected name " + name);
            }
        }
    }
}
