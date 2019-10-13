package com.orgzly.android.repos;

import com.orgzly.android.BookFormat;
import com.orgzly.android.BookName;
import com.orgzly.android.NotesOrgExporter;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.Book;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.util.MiscUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalDbRepoTest extends OrgzlyTest {
    @Before
    public void setUp() throws Exception {
        super.setUp();

        testUtils.setupBook("local-book-1", "Content\n\n* Note");
    }

    @Test
    public void testGetBooksFromAllRepos() throws IOException {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/mock-book.org", "book content\n\n* First note\n** Second note", "rev1", 1234567890000L);

        List<VersionedRook> books = SyncService.getBooksFromAllRepos(dataRepository, null);

        assertEquals(1, books.size());

        VersionedRook vrook = books.get(0);

        assertEquals("mock-book", BookName.getInstance(context, vrook).getName());
        assertEquals("mock://repo-a", vrook.getRepoUri().toString());
        assertEquals("mock://repo-a/mock-book.org", vrook.getUri().toString());
        assertEquals("rev1", vrook.getRevision());
        assertEquals(1234567890000L, vrook.getMtime());
    }

    @Test
    public void testStoringBook() throws IOException {
        SyncRepo repo;

        long now = System.currentTimeMillis();

        /* Write local book's content to a temporary file. */
        Book book = dataRepository.getBook("local-book-1");
        File tmpFile = dataRepository.getTempBookFile();

        try {
            new NotesOrgExporter(dataRepository).exportBook(book, tmpFile);
            repo = testUtils.repoInstance(RepoType.MOCK, "mock://repo-a");
            repo.storeBook(tmpFile, BookName.fileName(book.getName(), BookFormat.ORG));
        } finally {
            tmpFile.delete();
        }

        List<VersionedRook> books = repo.getBooks();
        assertEquals(1, books.size());

        VersionedRook vrook = books.get(0);
        assertEquals("local-book-1", BookName.getInstance(context, vrook).getName());
        assertEquals("mock://repo-a", vrook.getRepoUri().toString());
        assertTrue(vrook.getMtime() >= now);
    }

    @Test
    public void testRetrievingBook() throws IOException {
        Repo repo = testUtils.setupRepo(RepoType.MOCK, "mock://repo-a");
        testUtils.setupRook(repo, "mock://repo-a/mock-book.org", "book content\n\n* First note\n** Second note", "rev1", 1234567890000L);

        SyncRepo syncRepo = testUtils.repoInstance(RepoType.MOCK, "mock://repo-a");
        VersionedRook vrook = SyncService.getBooksFromAllRepos(dataRepository, null).get(0);

        File tmpFile = dataRepository.getTempBookFile();
        try {
            syncRepo.retrieveBook("mock-book.org", tmpFile);
            String content = MiscUtils.readStringFromFile(tmpFile);
            assertEquals("book content\n" + "\n" + "* First note\n" + "** Second note", content);
        } finally {
            tmpFile.delete();
        }
    }
}
