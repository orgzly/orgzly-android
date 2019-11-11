package com.orgzly.android.repos;

import android.net.Uri;

import com.orgzly.BuildConfig;
import com.orgzly.android.BookName;
import com.orgzly.android.LocalStorage;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.NoteView;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.sync.BookNamesake;
import com.orgzly.android.sync.BookSyncStatus;
import com.orgzly.android.util.EncodingDetect;
import com.orgzly.android.util.MiscUtils;

import org.junit.Before;
import org.junit.Test;

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

public class SyncTest extends OrgzlyTest {
    private static final String TAG = SyncTest.class.getName();

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AppPreferences.dropboxToken(context, BuildConfig.DROPBOX_TOKEN);
    }

    @Test
    public void testOrgRange() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(
                repo,
                "mock://repo-a/remote-book-1.org",
                "* Note\nSCHEDULED: <2015-01-13 уто 13:00-14:14>--<2015-01-14 сре 14:10-15:20>",
                "0abcdef",
                1400067156);

        testUtils.sync();

        NoteView noteView = dataRepository.getLastNoteView("Note");
        assertEquals(
                "<2015-01-13 уто 13:00-14:14>--<2015-01-14 сре 14:10-15:20>",
                noteView.getScheduledRangeString());
    }

    @Test
    public void testSync1() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("todo", "hum hum");

        assertEquals(1, dataRepository.getRepos().size());
        assertEquals(1, dataRepository.getBooks().size());
        assertNull(dataRepository.getBooks().get(0).getSyncedTo());

        testUtils.sync();

        assertEquals(1, dataRepository.getRepos().size());
        assertEquals(1, dataRepository.getBooks().size());
        assertNotNull(dataRepository.getBooks().get(0).getSyncedTo());
        assertEquals("mock://repo-a/todo.org", dataRepository.getBooks().get(0).getSyncedTo().getUri().toString());
    }

    @Test
    public void testSync2() {
        /* Add remote books. */
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/remote-book-1.org", "", "1abcdef", 1400067156);
        testUtils.setupRook(repo, "mock://repo-a/remote-book-2.org", "", "2abcdef", 1400067156);

        assertEquals(1, dataRepository.getRepos().size());
        assertEquals(2, dbRepoBookRepository.getBooks(repo.getId(), Uri.parse("mock://repo-a")).size());
        assertEquals("mock://repo-a", dbRepoBookRepository.getBooks(
                repo.getId(), Uri.parse("mock://repo-a")).get(0).getRepoUri().toString());
        assertEquals(0, dataRepository.getBooks().size());

        /* Sync. */
        Map<String, BookNamesake> g1 = testUtils.sync();
        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, g1.get("remote-book-1").getStatus());
        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, g1.get("remote-book-2").getStatus());

        assertEquals(1, dataRepository.getRepos().size());
        assertEquals(2, dbRepoBookRepository.getBooks(
                repo.getId(), Uri.parse("mock://repo-a")).size());
        assertEquals("mock://repo-a", dbRepoBookRepository.getBooks(
                repo.getId(), Uri.parse("mock://repo-a")).get(0).getRepoUri().toString());
        assertEquals(2, dataRepository.getBooks().size());

        /* Sync. */
        Map<String, BookNamesake> g2 = testUtils.sync();
        assertEquals(BookSyncStatus.NO_CHANGE, g2.get("remote-book-1").getStatus());
        assertEquals(BookSyncStatus.NO_CHANGE, g2.get("remote-book-2").getStatus());

        assertEquals(1, dataRepository.getRepos().size());
        assertEquals(2, dbRepoBookRepository.getBooks(
                repo.getId(), Uri.parse("mock://repo-a")).size());
        assertEquals("mock://repo-a", dbRepoBookRepository.getBooks(
                repo.getId(), Uri.parse("mock://repo-a")).get(0).getRepoUri().toString());
        assertEquals(2, dataRepository.getBooks().size());
    }

    @Test
    public void testRenameUsedRepo() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);

        BookView book;

        testUtils.sync();

        testUtils.renameRepo("mock://repo-a", "mock://repo-b");

        book = dataRepository.getBookView("book");
        assertNull(book.getLinkRepo());
        assertNull(book.getSyncedTo());

        testUtils.sync();

        book = dataRepository.getBookView("book");
        assertEquals(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO.toString(), book.getBook().getSyncStatus());
        assertEquals("mock://repo-b", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-b", book.getSyncedTo().getRepoUri().toString());
        assertEquals("mock://repo-b/book.org", book.getSyncedTo().getUri().toString());

        testUtils.renameRepo("mock://repo-b", "mock://repo-a");
        testUtils.sync();

        book = dataRepository.getBookView("book");
        assertEquals(BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST.toString(), book.getBook().getSyncStatus());
        assertNull(book.getLinkRepo());
        assertNull("mock://repo-b/book.org", book.getSyncedTo());
    }

    @Test
    public void testDeletingUsedRepo() {
        BookView book;

        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);
        testUtils.sync();

        testUtils.deleteRepo("mock://repo-a");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.sync();

        book = dataRepository.getBookView("book");
        assertEquals(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO.toString(), book.getBook().getSyncStatus());
        assertEquals("mock://repo-b", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-b", book.getSyncedTo().getRepoUri().toString());
        assertEquals("mock://repo-b/book.org", book.getSyncedTo().getUri().toString());

        testUtils.deleteRepo("mock://repo-b");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.sync();

        book = dataRepository.getBookView("book");
        assertEquals(BookSyncStatus.BOOK_WITHOUT_LINK_AND_ONE_OR_MORE_ROOKS_EXIST.toString(), book.getBook().getSyncStatus());
        assertNull(book.getLinkRepo());
        assertNull("mock://repo-b/book.org", book.getSyncedTo());
    }

    @Test
    public void testEncodingStaysTheSameAfterSecondSync() {
        BookView book;

        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/book.org", "Content A", "1abcde", 1400067156000L);

        testUtils.sync();

        book = dataRepository.getBooks().get(0);
        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK.toString(), book.getBook().getSyncStatus());

        switch (EncodingDetect.USED_METHOD) {
//            case JCHARDET:
//                assertEquals("ASCII", versionedRook.getDetectedEncoding());
//                assertEquals("ASCII", versionedRook.getUsedEncoding());
            case JUNIVERSALCHARDET:
                assertNull(book.getBook().getDetectedEncoding());
                assertEquals("UTF-8", book.getBook().getUsedEncoding());
                break;
        }
        assertNull(book.getBook().getSelectedEncoding());

        testUtils.sync();

        book = dataRepository.getBooks().get(0);
        assertEquals(BookSyncStatus.NO_CHANGE.toString(), book.getBook().getSyncStatus());

        switch (EncodingDetect.USED_METHOD) {
//            case JCHARDET:
//                assertEquals("ASCII", versionedRook.getDetectedEncoding());
//                assertEquals("ASCII", versionedRook.getUsedEncoding());
//                break;
            case JUNIVERSALCHARDET:
                assertNull(book.getBook().getDetectedEncoding());
                assertEquals("UTF-8", book.getBook().getUsedEncoding());
                break;
        }
        assertNull(book.getBook().getSelectedEncoding());
    }

    @Test
    public void testOnlyBookWithLink() {
        Repo repoA = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");

        BookView book = testUtils.setupBook("book-1", "Content");
        dataRepository.setLink(book.getBook().getId(), repoA);

        testUtils.sync();

        book = dataRepository.getBooks().get(0);
        assertEquals(BookSyncStatus.ONLY_BOOK_WITH_LINK.toString(), book.getBook().getSyncStatus());
    }

    @Test
    public void testMultipleRooks() {
        Repo repoA = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repoA, "mock://repo-a/book.org", "Content A", "revA", 1234567890000L);

        Repo repoB = testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupRook(repoB, "mock://repo-b/book.org", "Content B", "revB", 1234567890000L);

        testUtils.sync();

        BookView book = dataRepository.getBooks().get(0);

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS.toString(), book.getBook().getSyncStatus());
        assertTrue(book.getBook().isDummy());

        dataRepository.setLink(book.getBook().getId(), repoA);


        testUtils.sync();

        book = dataRepository.getBooks().get(0);

        assertEquals(BookSyncStatus.DUMMY_WITH_LINK.toString(), book.getBook().getSyncStatus());
        assertTrue(!book.getBook().isDummy());
        assertEquals("mock://repo-a/book.org", book.getSyncedTo().getUri().toString());
    }

    @Test
    public void testMtimeOfLoadedBook() {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/book.org", "Content", "rev", 1234567890000L);

        testUtils.sync();

        BookView book = dataRepository.getBooks().get(0);

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK.toString(), book.getBook().getSyncStatus());
        assertEquals(1234567890000L, book.getBook().getMtime().longValue());
    }

    @Test
    public void testDummyShouldNotBeSavedWhenHavingOneRepo() {
        Repo repoA = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        Repo repoB = testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupRook(repoA, "mock://repo-a/booky.org", "", "1abcdef", 1400067155);
        testUtils.setupRook(repoB, "mock://repo-b/booky.org", "", "2abcdef", 1400067156);

        Book book;
        Map<String, BookNamesake> namesakes;

        namesakes = testUtils.sync();
        book = dataRepository.getBook("booky");

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_MULTIPLE_ROOKS, namesakes.get("booky").getStatus());
        assertTrue(book.isDummy());

        testUtils.deleteRepo("mock://repo-a");
        testUtils.deleteRepo("mock://repo-b");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-c");

        namesakes = testUtils.sync();
        book = dataRepository.getBook("booky");

        assertEquals(BookSyncStatus.ONLY_DUMMY, namesakes.get("booky").getStatus());
        // TODO: We should delete it, no point of having a dummy and no remote book

        assertTrue(book.isDummy());
    }

    @Test
    public void testDeletedRepoShouldStayAsBookLink() {
        Repo repoA = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        testUtils.setupRook(repoA, "mock://repo-a/booky.org", "", "1abcdef", 1400067155);

        BookView book;
        Map<String, BookNamesake> namesakes;

        namesakes = testUtils.sync();
        book = dataRepository.getBookView("booky");

        assertEquals(BookSyncStatus.DUMMY_WITHOUT_LINK_AND_ONE_ROOK, namesakes.get("booky").getStatus());

        assertFalse(book.getBook().isDummy());
        assertEquals("mock://repo-a", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-a", book.getSyncedTo().getRepoUri().toString());

        testUtils.deleteRepo("mock://repo-a");
        testUtils.deleteRepo("mock://repo-b");
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-c");

        namesakes = testUtils.sync(); // TODO: Don't use namesakes, be consistent and use book.status like in some methods
        book = dataRepository.getBookView("booky");

        assertEquals(BookSyncStatus.ONLY_BOOK_WITHOUT_LINK_AND_ONE_REPO, namesakes.get("booky").getStatus());

        assertFalse(book.getBook().isDummy());
        assertEquals("mock://repo-c", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-c", book.getSyncedTo().getRepoUri().toString());
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
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/booky.org.txt", "", "1abcdef", 1400067155);

        testUtils.sync();

        BookView book = dataRepository.getBookView("booky");
        assertEquals("mock://repo-a", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-a", book.getSyncedTo().getRepoUri().toString());
        assertEquals("mock://repo-a/booky.org.txt", book.getSyncedTo().getUri().toString());
    }

    @Test
    public void testMockFileRename() throws IOException {
        List<VersionedRook> vrooks;

        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        SyncRepo repo = testUtils.repoInstance(RepoType.MOCK, "mock://repo-a");

        BookView book = testUtils.setupBook("Booky", "1 2 3");

        testUtils.sync();

        vrooks = repo.getBooks();

        assertEquals(1, vrooks.size());
        assertEquals("Booky", BookName.getInstance(context, vrooks.get(0)).getName());

        long mtime = vrooks.get(0).getMtime();
        String rev = vrooks.get(0).getRevision();

        // Rename local notebook
        dataRepository.renameBook(book, "BookyRenamed");

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
        String uuid = UUID.randomUUID().toString();

        String repoDir = context.getCacheDir() + "/" + uuid;

        SyncRepo repo = testUtils.repoInstance(RepoType.DIRECTORY, "file:" + repoDir);

        assertNotNull(repo);
        assertEquals(0, repo.getBooks().size());

        File file = File.createTempFile("notebook.", ".org");
        MiscUtils.writeStringToFile("1 2 3", file);

        VersionedRook vrook = repo.storeBook(file, file.getName());

        file.delete();

        assertEquals(1, repo.getBooks().size());

        repo.renameBook(vrook.getUri(), "notebook-renamed");

        assertEquals(1, repo.getBooks().size());
        assertEquals(repo.getUri() + "/notebook-renamed.org", repo.getBooks().get(0).getUri().toString());
        assertEquals("notebook-renamed.org", BookName.getInstance(context, repo.getBooks().get(0)).getFileName());

        LocalStorage.deleteRecursive(new File(repoDir));
    }

    @Test
    public void testRenameSyncedBook() {
        testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupBook("Booky", "1 2 3");

        testUtils.sync();

        BookView book = dataRepository.getBookView("Booky");

        assertEquals("mock://repo-a/Booky.org", book.getSyncedTo().getUri().toString());

        dataRepository.renameBook(book, "BookyRenamed");

        BookView renamedBook = dataRepository.getBookView("BookyRenamed");

        assertNotNull(renamedBook);
        assertEquals("mock://repo-a", renamedBook.getLinkRepo().getUrl());
        assertEquals("mock://repo-a", renamedBook.getSyncedTo().getRepoUri().toString());
        assertEquals("mock://repo-a/BookyRenamed.org", renamedBook.getSyncedTo().getUri().toString());
    }

    @Test
    public void testRenameSyncedBookWithDifferentLink() throws IOException {
        BookView book;

        Repo repoA = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        Repo repoB = testUtils.setupRepo(RepoType.MOCK, "mock://repo-b");
        book = testUtils.setupBook("Booky", "1 2 3");
        dataRepository.setLink(book.getBook().getId(), repoA);

        testUtils.sync();

        book = dataRepository.getBooks().get(0);

        assertEquals(1, testUtils.repoInstance(RepoType.MOCK, "mock://repo-a").getBooks().size());
        assertEquals(0, testUtils.repoInstance(RepoType.MOCK, "mock://repo-b").getBooks().size());
        assertEquals("mock://repo-a", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-a", book.getSyncedTo().getRepoUri().toString());
        assertEquals("mock://repo-a/Booky.org", book.getSyncedTo().getUri().toString());

        dataRepository.setLink(book.getBook().getId(), repoB);

        book = dataRepository.getBooks().get(0);

        dataRepository.renameBook(book, "BookyRenamed");

        book = dataRepository.getBooks().get(0);

        assertEquals("Booky", book.getBook().getName());
        assertEquals(BookSyncStatus.ROOK_AND_VROOK_HAVE_DIFFERENT_REPOS.toString(), book.getBook().getSyncStatus());
        assertEquals("mock://repo-b", book.getLinkRepo().getUrl());
        assertEquals("mock://repo-a/Booky.org", book.getSyncedTo().getUri().toString());
    }
}
