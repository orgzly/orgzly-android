package com.orgzly.android.repos;

import com.orgzly.BuildConfig;
import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.util.MiscUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DropboxRepoTest extends OrgzlyTest {
    private static final String DROPBOX_TEST_DIR = "/orgzly-android-tests";

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AppPreferences.dropboxToken(context, BuildConfig.DROPBOX_TOKEN);
    }

    @Test
    public void testUrl() {
        assertEquals("dropbox:/dir", RepoFactory.getFromUri(context, "dropbox:/dir").getUri().toString());
    }

    @Test
    public void testSyncingUrlWithTrailingSlash() throws IOException {
        shelfTestUtils.setupRepo(randomUrl() + "/");
        assertNotNull(shelf.sync());
    }

    /* Dropbox repo url should *not* have authority. */
    @Test
    public void testAuthority() {
        assertNull(RepoFactory.getFromUri(context, "dropbox://authority"));
    }

    @Test
    public void testRenameBook() throws IOException {
        Book book;
        String repoUriString = RepoFactory.getFromUri(context, randomUrl()).getUri().toString();

        shelfTestUtils.setupRepo(repoUriString);
        shelfTestUtils.setupBook("booky", "");

        shelf.sync();
        book = shelf.getBook("booky");

        assertEquals(repoUriString, book.getLinkRepo().toString());
        assertEquals(repoUriString, book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals(repoUriString + "/booky.org", book.getLastSyncedToRook().getUri().toString());

        shelf.renameBook(book, "booky-renamed");
        book = shelf.getBook("booky-renamed");

        assertEquals(repoUriString, book.getLinkRepo().toString());
        assertEquals(repoUriString, book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals(repoUriString + "/booky-renamed.org", book.getLastSyncedToRook().getUri().toString());
    }

    @Test
    public void testDropboxFileRename() throws IOException {
        Repo repo = RepoFactory.getFromUri(context, randomUrl());

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

    private String randomUrl() {
        return "dropbox:"+ DROPBOX_TEST_DIR + "/" + UUID.randomUUID().toString();
    }
}
