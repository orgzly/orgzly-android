package com.orgzly.android.repos;

import android.os.Environment;

import com.orgzly.android.Book;
import com.orgzly.android.BookName;
import com.orgzly.android.LocalStorage;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.util.MiscUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DirectoryRepoTest extends OrgzlyTest {
    private static final String TAG = DirectoryRepoTest.class.getName();

    private File dirFile;
    private String repoUriString;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        dirFile = new LocalStorage(context).getLocalRepoDirectory("orgzly-local-dir-repo-test");
        repoUriString = "file:" + dirFile.getAbsolutePath();

        LocalStorage.deleteRecursive(dirFile);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        LocalStorage.deleteRecursive(dirFile);
    }

    @Test
    public void testStoringFile() throws IOException {
        Repo repo = RepoFactory.getFromUri(context, repoUriString);

        File tmpFile = shelf.getTempBookFile();
        try {
            MiscUtils.writeStringToFile("...", tmpFile);
            repo.storeBook(tmpFile, "booky.org");
        } finally {
            tmpFile.delete();
        }

        List<VersionedRook> books = repo.getBooks();

        assertEquals(1, books.size());
        assertEquals("booky", BookName.getInstance(context, books.get(0)).getName());
        assertEquals("booky.org", BookName.getInstance(context, books.get(0)).getFileName());
        assertEquals(repoUriString, books.get(0).getRepoUri().toString());
        assertEquals(repoUriString + "/booky.org", books.get(0).getUri().toString());
    }

    @Test
    public void testExtension() throws IOException {
        DirectoryRepo repo = new DirectoryRepo(repoUriString, true);
        MiscUtils.writeStringToFile("Notebook content 1", new File(dirFile, "01.txt"));
        MiscUtils.writeStringToFile("Notebook content 2", new File(dirFile, "02.o"));
        MiscUtils.writeStringToFile("Notebook content 3", new File(dirFile, "03.org"));

        List<VersionedRook> books = repo.getBooks();

        assertEquals(1, books.size());
        assertEquals("03", BookName.getInstance(context, books.get(0)).getName());
        assertEquals("03.org", BookName.getInstance(context, books.get(0)).getFileName());
        assertEquals(repoUriString, books.get(0).getRepoUri().toString());
        assertEquals(repoUriString + "/03.org", books.get(0).getUri().toString());
    }

    @Test
    public void testListDownloadsDirectory() throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path = DirectoryRepo.SCHEME + ":" + dir.getAbsolutePath();

        DirectoryRepo repo = new DirectoryRepo(path, false);

        assertNotNull(repo.getBooks());
    }

    // TODO: Do the same for dropbox repo
    @Test
    public void testRenameBook() throws IOException {
        Book book;

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
    public void testSyncWithDirectoryContainingPercent() throws FileNotFoundException {
        String localBaseDir = context.getExternalCacheDir().getAbsolutePath();
        String localDir = localBaseDir + "/nextcloud/user@host%2Fdir/space separated";
        String localDirEnc = localBaseDir + "/nextcloud/user%40host%252Fdir/space%20separated";
        new File(localDir).mkdirs();

        MiscUtils.writeStringToFile("Notebook content 1", new File(localDir, "notebook.org"));

        DirectoryRepo repo = (DirectoryRepo) shelfTestUtils.setupRepo("file:" + localDirEnc);

        shelf.sync();

        assertEquals("file:" + localDirEnc, repo.getUri().toString());
        assertEquals(localDir, repo.getDirectory().toString());
        assertEquals(1, shelf.getBooks().size());
    }

    // TODO: Test saving and loading
}
