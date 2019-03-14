package com.orgzly.android;

import android.content.Context;
import android.net.Uri;

import com.orgzly.android.data.DataRepository;
import com.orgzly.android.data.DbRepoBookRepository;
import com.orgzly.android.db.entity.BookAction;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.repos.SyncRepo;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.sync.BookNamesake;
import com.orgzly.android.sync.SyncService;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Utility methods used by tests.
 * Creating and checking books, rooks, encodings etc.
 */
public class TestUtils {
    private Context context;
    private DataRepository dataRepository;
    private RepoFactory repoFactory;
    private DbRepoBookRepository dbRepoBookRepository;

    public TestUtils(
            Context context,
            DataRepository dataRepository,
            RepoFactory repoFactory,
            DbRepoBookRepository dbRepoBookRepository) {

        this.context = context;
        this.dataRepository = dataRepository;
        this.repoFactory = repoFactory;
        this.dbRepoBookRepository = dbRepoBookRepository;
    }

    public SyncRepo setupRepo(String url) {
        dataRepository.createRepo(url);

        return repoFactory.getFromUri(context, url);
    }

    public void deleteRepo(String url) {
        Repo repo = dataRepository.getRepo(url);
        if (repo != null) {
            dataRepository.deleteRepo(repo.getId());
        }
    }

    public void renameRepo(String fromUrl, String toUrl) {
        Repo repo = dataRepository.getRepo(fromUrl);
        if (repo != null) {
            dataRepository.updateRepo(repo.getId(), toUrl);
        } else {
            throw new IllegalStateException("Repo " + fromUrl + " does not exist");
        }
    }

    public BookView setupBook(String name, String content) {
        BookView bookView = null;
        try {
            bookView = loadBookFromContent(name, BookFormat.ORG, content, null);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        return bookView;
    }

    public BookView setupBook(String name, String content, String linkRepoUrl) {
        BookView bookView = null;
        try {
            bookView = loadBookFromContent(name, BookFormat.ORG, content, null);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        dataRepository.setLink(bookView.getBook().getId(), linkRepoUrl);
        return bookView;
    }

    /**
     * Overwrites existing repoUrl / url combinations (due to table definition).
     */
    public void setupRook(String repoUrl, String url, String content, String rev, long mtime) {
        VersionedRook vrook = new VersionedRook(Uri.parse(repoUrl), Uri.parse(url), rev, mtime);
        dbRepoBookRepository.createBook(vrook, content);

        // RemoteBookRevision remoteBookRevision = new RemoteBookRevision(repoUrl, url, rev, mtime);
        // RemoteBooksHelper.updateOrInsert(testContext, remoteBookRevision);
    }

    public void assertBook(String name, String expectedContent) {
        assertEquals(expectedContent, getBookContent(name));
    }

    private String getBookContent(String name) {
        try {
            return dataRepository.getBookContent(name, BookFormat.ORG);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Imports book to database overwriting the existing one with the same name.
     * @param name Notebook name
     * @param content Notebook's content
     */
    private BookView loadBookFromContent(String name, BookFormat format, String content, VersionedRook vrook) throws IOException {
        /* Save content to temporary file. */
        File tmpFile = dataRepository.getTempBookFile();
        MiscUtils.writeStringToFile(content, tmpFile);

        try {
            return dataRepository.loadBookFromFile(name, format, tmpFile, vrook);

        } finally {
            /* Delete temporary file. */
            tmpFile.delete();
        }
    }

    public Map<String, BookNamesake> sync() {
        try {
            Map<String, BookNamesake> nameGroups = SyncService.groupAllNotebooksByName(dataRepository);

            for (BookNamesake group : nameGroups.values()) {
                BookAction action = SyncService.syncNamesake(dataRepository, group);
                dataRepository.setBookLastActionAndSyncStatus(
                        group.getBook().getBook().getId(), action, group.getStatus().toString());
            }

            return nameGroups;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
