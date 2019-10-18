package com.orgzly.android.repos;

import android.os.Environment;

import com.orgzly.android.BookName;
import com.orgzly.android.LocalStorage;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.Repo;
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

        dirFile = localStorage.getCacheDirectory("orgzly-local-dir-repo-test");
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
        testUtils.setupRepo(RepoType.DIRECTORY, repoUriString);
        SyncRepo repo = testUtils.repoInstance(RepoType.DIRECTORY, repoUriString);

        File tmpFile = dataRepository.getTempBookFile();
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
        RepoWithProps repoWithProps = new RepoWithProps(new Repo(13, RepoType.DIRECTORY, repoUriString));
        DirectoryRepo repo = new DirectoryRepo(repoWithProps, true);
        MiscUtils.writeStringToFile("Notebook content 1", new File(dirFile, "01.txt"));
        MiscUtils.writeStringToFile("Notebook content 2", new File(dirFile, "02.o"));
        MiscUtils.writeStringToFile("Notebook content 3", new File(dirFile, "03.org"));

        List<VersionedRook> books = repo.getBooks();

        assertEquals(1, books.size());
        assertEquals("03", BookName.getInstance(context, books.get(0)).getName());
        assertEquals("03.org", BookName.getInstance(context, books.get(0)).getFileName());
        assertEquals(13, books.get(0).getRepoId());
        assertEquals(repoUriString, books.get(0).getRepoUri().toString());
        assertEquals(repoUriString + "/03.org", books.get(0).getUri().toString());
    }

    @Test
    public void testListDownloadsDirectory() throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path = DirectoryRepo.SCHEME + ":" + dir.getAbsolutePath();

        RepoWithProps repoWithProps = new RepoWithProps(new Repo(13, RepoType.DIRECTORY, path));
        DirectoryRepo repo = new DirectoryRepo(repoWithProps, false);

        assertNotNull(repo.getBooks());
    }

    // TODO: Do the same for dropbox repo
    @Test
    public void testRenameBook() {
        BookView bookView;

        testUtils.setupRepo(RepoType.DIRECTORY, repoUriString);
        testUtils.setupBook("booky", "");

        testUtils.sync();
        bookView = dataRepository.getBookView("booky");

        assertEquals(repoUriString, bookView.getLinkRepo().getUrl());
        assertEquals(repoUriString, bookView.getSyncedTo().getRepoUri().toString());
        assertEquals(repoUriString + "/booky.org", bookView.getSyncedTo().getUri().toString());

        dataRepository.renameBook(bookView, "booky-renamed");
        bookView = dataRepository.getBookView("booky-renamed");

        assertEquals(repoUriString, bookView.getLinkRepo().getUrl());
        assertEquals(repoUriString, bookView.getSyncedTo().getRepoUri().toString());
        assertEquals(repoUriString + "/booky-renamed.org", bookView.getSyncedTo().getUri().toString());
    }


    @Test
    public void testSyncWithDirectoryContainingPercent() throws FileNotFoundException {
        String localBaseDir = context.getExternalCacheDir().getAbsolutePath();
        String nextcloudDir = localBaseDir + "/nextcloud";
        String localDir = nextcloudDir + "/user@host%2Fdir/space separated";
        String localDirEnc = nextcloudDir + "/user%40host%252Fdir/space%20separated";
        new File(localDir).mkdirs();

        MiscUtils.writeStringToFile("Notebook content 1", new File(localDir, "notebook.org"));

        testUtils.setupRepo(RepoType.DIRECTORY, "file:" + localDirEnc);
        DirectoryRepo repo = (DirectoryRepo) testUtils.repoInstance(RepoType.DIRECTORY, "file:" + localDirEnc);

        testUtils.sync();

        assertEquals("file:" + localDirEnc, repo.getUri().toString());
        assertEquals(localDir, repo.getDirectory().toString());
        assertEquals(1, dataRepository.getBooks().size());

        LocalStorage.deleteRecursive(new File(nextcloudDir));
    }

    // TODO: Test saving and loading
}
