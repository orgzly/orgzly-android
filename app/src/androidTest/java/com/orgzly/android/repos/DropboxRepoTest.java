package com.orgzly.android.repos;

import com.orgzly.BuildConfig;
import com.orgzly.android.Book;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.prefs.AppPreferences;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class DropboxRepoTest extends OrgzlyTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        AppPreferences.dropboxToken(context, BuildConfig.DROPBOX_TOKEN);
    }

    @Test
    public void testUrl() {
        assertEquals("dropbox:/dir", RepoFactory.getFromUri(context, "dropbox:/dir").getUri().toString());
    }

    /**
     * Dropbox repo url should not have authority.
     */
    @Test
    public void testAuthority() {
        assertNull(RepoFactory.getFromUri(context, "dropbox://authority"));
    }

    @Test
    public void testRenameBook() throws IOException {
        Book book;
        String repoUriString = randomDropboxRepo(context).getUri().toString();

        shelfTestUtils.setupRepo(repoUriString);
        shelfTestUtils.setupBook("booky", "");

        shelf.sync();
        book = shelf.getBook("booky");

        assertEquals(repoUriString, book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals(repoUriString + "/booky.org", book.getLastSyncedToRook().getUri().toString());
        assertEquals(repoUriString + "/booky.org", book.getLink().getUri().toString());

        shelf.renameBook(book, "booky-renamed");
        book = shelf.getBook("booky-renamed");

        assertEquals(repoUriString, book.getLastSyncedToRook().getRepoUri().toString());
        assertEquals(repoUriString + "/booky-renamed.org", book.getLastSyncedToRook().getUri().toString());
        assertEquals(repoUriString + "/booky-renamed.org", book.getLink().getUri().toString());
    }
}
