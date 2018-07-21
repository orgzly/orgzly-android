package com.orgzly.android.repos;

import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import com.orgzly.BuildConfig;
import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.Note;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.clients.CurrentRooksClient;
import com.orgzly.android.provider.clients.LocalDbRepoClient;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.sync.BookNamesake;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.android.util.EncodingDetect;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.datetime.OrgRange;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SyncTest extends OrgzlyTest {
    private static final String TAG = SyncTest.class.getName();

    private Repo randomDirectoryRepo() {
        String uuid = UUID.randomUUID().toString();
        return RepoFactory.getFromUri(context, "file:" + context.getCacheDir() + "/" + uuid);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AppPreferences.dropboxToken(context, BuildConfig.DROPBOX_TOKEN);
    }

    @Test
    public void testOrgRange() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook(
                "mock://repo-a",
                "mock://repo-a/remote-book-1.org",
                "* Note\nSCHEDULED: <2015-01-13 уто 13:00-14:14>--<2015-01-14 сре 14:10-15:20>",
                "0abcdef",
                1400067156);

        shelf.sync();

        Note note = shelf.getNote("Note");
        OrgRange range = note.getHead().getScheduled();
        assertEquals("<2015-01-13 уто 13:00-14:14>--<2015-01-14 сре 14:10-15:20>", range.toString());
    }

    @Test
    public void testSync1() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupBook("todo", "hum hum");

        assertEquals(1, ReposClient.getAll(context).size());
        assertEquals(1, shelf.getBooks().size());
        assertEquals(0, CurrentRooksClient.getAll(context).size());
        assertNull(shelf.getBooks().get(0).getLastSyncedToRook());

        shelf.sync();

        assertEquals(1, ReposClient.getAll(context).size());
        assertEquals(1, shelf.getBooks().size());
        assertEquals(1, CurrentRooksClient.getAll(context).size());
        assertNotNull(shelf.getBooks().get(0).getLastSyncedToRook());
        assertEquals("mock://repo-a/todo.org", shelf.getBooks().get(0).getLastSyncedToRook().getUri().toString());
    }

    @Test
    public void testSync2() {
        /* Add remote books. */
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-1.org", "", "1abcdef", 1400067156);
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/remote-book-2.org", "", "2abcdef", 1400067156);

        assertEquals(1, ReposClient.getAll(context).size());
        assertEquals(2, LocalDbRepoClient.getAll(context, Uri.parse("mock://repo-a")).size());
        assertEquals("mock://repo-a", LocalDbRepoClient.getAll(context, Uri.parse("mock://repo-a")).get(0).getRepoUri().toString());
        assertEquals(0, CurrentRooksClient.getAll(context).size());
        assertEquals(0, shelf.getBooks().size());

        /* Sync. */
        Map<String, BookNamesake> g1 = shelf.sync();
        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, g1.get("remote-book-1").getStatus());
        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, g1.get("remote-book-2").getStatus());

        assertEquals(1, ReposClient.getAll(context).size());
        assertEquals(2, LocalDbRepoClient.getAll(context, Uri.parse("mock://repo-a")).size());
        assertEquals("mock://repo-a", LocalDbRepoClient.getAll(context, Uri.parse("mock://repo-a")).get(0).getRepoUri().toString());
        assertEquals(2, CurrentRooksClient.getAll(context).size());
        assertEquals(2, shelf.getBooks().size());

        /* Sync. */
        Map<String, BookNamesake> g2 = shelf.sync();
        assertEquals(BookSyncStatus.NO_CHANGE, g2.get("remote-book-1").getStatus());
        assertEquals(BookSyncStatus.NO_CHANGE, g2.get("remote-book-2").getStatus());

        assertEquals(1, ReposClient.getAll(context).size());
        assertEquals(2, LocalDbRepoClient.getAll(context, Uri.parse("mock://repo-a")).size());
        assertEquals("mock://repo-a", LocalDbRepoClient.getAll(context, Uri.parse("mock://repo-a")).get(0).getRepoUri().toString());
        assertEquals(2, CurrentRooksClient.getAll(context).size());
        assertEquals(2, shelf.getBooks().size());
    }

    @Test
    public void testRenameUsedRepo() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);

        Book book;

        shelf.sync();

        shelfTestUtils.renameRepo("mock://repo-a", "mock://repo-b");

        book = shelf.getBook("book");
        assertNull(book.getLinkRepo());
        assertEquals("mock://repo-a/book.org", book.getLastSyncedToRook().getUri().toString());
        assertEquals("mock://repo-a", book.getLastSyncedToRook().getRepoUri().toString());

        shelf.sync();

        book = shelf.getBook("book");
        assertEquals(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO, book.getSyncStatus());
        assertEquals("mock://repo-b", book.getLinkRepo().toString());
        assertEquals("mock://repo-b", book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals("mock://repo-b/book.org", book.getLastSyncedToRook().getUri().toString());

        shelfTestUtils.renameRepo("mock://repo-b", "mock://repo-a");
        shelf.sync();

        book = shelf.getBook("book");
        assertEquals(BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST, book.getSyncStatus());
        assertNull(book.getLinkRepo());
        assertEquals("mock://repo-b/book.org", book.getLastSyncedToRook().getUri().toString());
        assertEquals("mock://repo-b", book.getLastSyncedToRook().getRepoUri().toString());
    }

    @Test
    public void testDeletingUsedRepo() {
        Book book;

        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);
        shelf.sync();

        shelfTestUtils.deleteRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-b");
        shelf.sync();

        book = shelf.getBook("book");
        assertEquals(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO, book.getSyncStatus());
        assertEquals("mock://repo-b", book.getLinkRepo().toString());
        assertEquals("mock://repo-b", book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals("mock://repo-b/book.org", book.getLastSyncedToRook().getUri().toString());

        shelfTestUtils.deleteRepo("mock://repo-b");
        shelfTestUtils.setupRepo("mock://repo-a");
        shelf.sync();

        book = shelf.getBook("book");
        assertEquals(BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST, book.getSyncStatus());
        assertNull(book.getLinkRepo());
        assertEquals("mock://repo-b/book.org", book.getLastSyncedToRook().getUri().toString());
    }

    @Test
    public void testEncodingStaysTheSameAfterSecondSync() {
        Book book;

        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);

        shelf.sync();

        book = shelf.getBooks().get(0);
        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, book.getSyncStatus());

        switch (EncodingDetect.USED_METHOD) {
//            case JCHARDET:
//                assertEquals("ASCII", versionedRook.getDetectedEncoding());
//                assertEquals("ASCII", versionedRook.getUsedEncoding());
            case JUNIVERSALCHARDET:
                assertNull(book.getDetectedEncoding());
                assertEquals("UTF-8", book.getUsedEncoding());
                break;
        }
        assertNull(book.getSelectedEncoding());

        shelf.sync();

        book = shelf.getBooks().get(0);
        assertEquals(BookSyncStatus.NO_CHANGE, book.getSyncStatus());

        switch (EncodingDetect.USED_METHOD) {
//            case JCHARDET:
//                assertEquals("ASCII", versionedRook.getDetectedEncoding());
//                assertEquals("ASCII", versionedRook.getUsedEncoding());
//                break;
            case JUNIVERSALCHARDET:
                assertNull(book.getDetectedEncoding());
                assertEquals("UTF-8", book.getUsedEncoding());
                break;
        }
        assertNull(book.getSelectedEncoding());
    }

    @Test
    public void testOnlyBookWithLink() {
        shelfTestUtils.setupRepo("mock://repo-a");

        Book book = shelfTestUtils.setupBook("book-1", "Content");
        shelfTestUtils.setBookLink(book.getId(), "mock://repo-a");

        shelf.sync();

        book = shelf.getBooks().get(0);
        assertEquals(BookSyncStatus.ONLY_BOOK_WITH_LINK, book.getSyncStatus());
    }

    @Test
    public void testMultipleRooks() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book.org", "Content A", "revA", 1234567890000L);

        shelfTestUtils.setupRepo("mock://repo-b");
        shelfTestUtils.setupRook("mock://repo-b", "mock://repo-b/book.org", "Content B", "revB", 1234567890000L);

        shelf.sync();

        Book book = shelf.getBooks().get(0);

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS, book.getSyncStatus());
        assertTrue(book.isDummy());

        shelfTestUtils.setBookLink(book.getId(), "mock://repo-a");

        shelf.sync();

        book = shelf.getBooks().get(0);

        assertEquals(BookSyncStatus.DUMMY_WITH_LINK, book.getSyncStatus());
        assertTrue(!book.isDummy());
        assertEquals("mock://repo-a/book.org", book.getLastSyncedToRook().getUri().toString());
    }

    @Test
    public void testMtimeOfLoadedBook() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/book.org", "Content", "rev", 1234567890000L);

        shelf.sync();

        Book book = shelf.getBooks().get(0);

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, book.getSyncStatus());
        assertEquals(0, book.getMtime());
    }

    @Test
    public void testDummyShouldNotBeSavedWhenHavingOneRepo() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-b");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org", "", "1abcdef", 1400067155);
        shelfTestUtils.setupRook("mock://repo-b", "mock://repo-b/booky.org", "", "2abcdef", 1400067156);

        Book book;
        Map<String, BookNamesake> namesakes;

        namesakes = shelf.sync();
        book = shelf.getBook("booky");

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS, namesakes.get("booky").getStatus());
        assertTrue(book.isDummy());

        shelfTestUtils.deleteRepo("mock://repo-a");
        shelfTestUtils.deleteRepo("mock://repo-b");
        shelfTestUtils.setupRepo("mock://repo-c");

        namesakes = shelf.sync();
        book = shelf.getBook("booky");

        assertEquals(BookSyncStatus.ONLY_DUMMY, namesakes.get("booky").getStatus()); // TODO: We should delete it, no point of having a dummy and no remote book
        assertTrue(book.isDummy());

    }

    @Test
    public void testDeletedRepoShouldStayAsBookLink() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRepo("mock://repo-b");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org", "", "1abcdef", 1400067155);

        Book book;
        Map<String, BookNamesake> namesakes;

        namesakes = shelf.sync();
        book = shelf.getBook("booky");

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, namesakes.get("booky").getStatus());

        assertFalse(book.isDummy());
        assertEquals("mock://repo-a", book.getLinkRepo().toString());
        assertEquals("mock://repo-a", book.getLastSyncedToRook().getRepoUri().toString());

        shelfTestUtils.deleteRepo("mock://repo-a");
        shelfTestUtils.deleteRepo("mock://repo-b");
        shelfTestUtils.setupRepo("mock://repo-c");

        namesakes = shelf.sync(); // TODO: Don't use namesakes, be consistent and use book.status like in some methods
        book = shelf.getBook("booky");

        assertEquals(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO, namesakes.get("booky").getStatus());

        assertFalse(book.isDummy());
        assertEquals("mock://repo-c", book.getLinkRepo().toString());
        assertEquals("mock://repo-c", book.getLastSyncedToRook().getRepoUri().toString());
    }

//    public void testEncodingOnSyncSavingStaysTheSame() {
//        setup.setupRepo("mock://repo-a");
//        setup.setupRook("mock://repo-a", "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);
//        sync();
//        setup.renameRepo("mock://repo-a", "mock://repo-b");
//        sync();
//        VersionedRook vrook = CurrentRooksHelper.get(testContext, "mock://repo-b/book.org");
//        assertNull(vrook.getDetectedEncoding());
//        assertEquals("UTF-8", vrook.getUsedEncoding());
//
//    }

    @Test
    public void testSyncingOrgTxt() {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupRook("mock://repo-a", "mock://repo-a/booky.org.txt", "", "1abcdef", 1400067155);

        shelf.sync();

        Book book = shelf.getBook("booky");
        assertEquals("mock://repo-a", book.getLinkRepo().toString());
        assertEquals("mock://repo-a", book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals("mock://repo-a/booky.org.txt", book.getLastSyncedToRook().getUri().toString());
    }

    @Test
    public void testMockFileRename() throws IOException {
        List<VersionedRook> vrooks;

        Repo repo = shelfTestUtils.setupRepo("mock://repo-a");
        Book book = shelfTestUtils.setupBook("Booky", "1 2 3");

        shelf.sync();

        vrooks = repo.getBooks();

        assertEquals(1, vrooks.size());
        assertEquals("Booky", BookName.getInstance(context, vrooks.get(0)).getName());

        long mtime = vrooks.get(0).getMtime();
        String rev = vrooks.get(0).getRevision();

        // Rename local notebook
        shelf.renameBook(book, "BookyRenamed");

        // Rename rook
        repo.renameBook(Uri.parse("mock://repo-a/Booky.org"), "BookyRenamed");

        vrooks = repo.getBooks();

        assertEquals(1, vrooks.size());
        assertEquals("BookyRenamed", BookName.getInstance(context, vrooks.get(0)).getName());
        assertEquals("mock://repo-a/BookyRenamed.org", vrooks.get(0).getUri().toString());
        assertTrue(mtime < vrooks.get(0).getMtime());
        assertNotSame(rev, vrooks.get(0).getRevision());
    }

    @Test
    public void testDirectoryFileRename() throws IOException {
        Repo repo = randomDirectoryRepo();

        assertNotNull(repo);
        assertEquals(0, repo.getBooks().size());

        File file = File.createTempFile("notebook.", ".org");
        MiscUtils.writeStringToFile("1 2 3", file);

        VersionedRook vrook = repo.storeBook(file, file.getName());

        assertEquals(1, repo.getBooks().size());

        repo.renameBook(vrook.getUri(), "notebook-renamed");

        assertEquals(1, repo.getBooks().size());
        assertEquals(repo.getUri() + "/notebook-renamed.org", repo.getBooks().get(0).getUri().toString());
        assertEquals("notebook-renamed.org", BookName.getInstance(context, repo.getBooks().get(0)).getFileName());
    }

    @Test
    public void testRenameSyncedBook() throws IOException {
        shelfTestUtils.setupRepo("mock://repo-a");
        shelfTestUtils.setupBook("Booky", "1 2 3");

        shelf.sync();

        Book book = BooksClient.get(context, "Booky");

        assertEquals("mock://repo-a/Booky.org", book.getLastSyncedToRook().getUri().toString());

        shelf.renameBook(book, "BookyRenamed");

        Book renamedBook = BooksClient.get(context, "BookyRenamed");

        assertNotNull(renamedBook);
        assertEquals("mock://repo-a", renamedBook.getLinkRepo().toString());
        assertEquals("mock://repo-a", renamedBook.getLastSyncedToRook().getRepoUri().toString());
        assertEquals("mock://repo-a/BookyRenamed.org", renamedBook.getLastSyncedToRook().getUri().toString());
    }

    @Test
    public void testRenameSyncedBookWithDifferentLink() throws IOException {
        Book book;

        Repo repoA = shelfTestUtils.setupRepo("mock://repo-a");
        Repo repoB = shelfTestUtils.setupRepo("mock://repo-b");
        book = shelfTestUtils.setupBook("Booky", "1 2 3");
        shelf.setLink(book, "mock://repo-a");

        shelf.sync();

        book = BooksClient.getAll(context).get(0);

        assertEquals(1, repoA.getBooks().size());
        assertEquals(0, repoB.getBooks().size());
        assertEquals("mock://repo-a", book.getLinkRepo().toString());
        assertEquals("mock://repo-a", book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals("mock://repo-a/Booky.org", book.getLastSyncedToRook().getUri().toString());

        shelf.setLink(book, "mock://repo-b");

        book = BooksClient.getAll(context).get(0);

        shelf.renameBook(book, "BookyRenamed");

        book = BooksClient.getAll(context).get(0);

        assertEquals("Booky", book.getName());
        assertEquals(BookSyncStatus.ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS, book.getSyncStatus());
        assertEquals("mock://repo-b", book.getLinkRepo().toString());
        assertEquals("mock://repo-a/Booky.org", book.getLastSyncedToRook().getUri().toString());
    }
}
