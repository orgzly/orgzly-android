package com.orgzly.android;

import android.net.Uri;

import com.orgzly.android.data.DataRepository;
import com.orgzly.android.data.DbRepoBookRepository;
import com.orgzly.android.db.entity.BookAction;
import com.orgzly.android.db.entity.BookView;
import com.orgzly.android.db.entity.Repo;
import com.orgzly.android.repos.RepoType;
import com.orgzly.android.repos.RepoWithProps;
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
    private DataRepository dataRepository;
    private DbRepoBookRepository dbRepoBookRepository;

    TestUtils(DataRepository dataRepository, DbRepoBookRepository dbRepoBookRepository) {
        this.dataRepository = dataRepository;
        this.dbRepoBookRepository = dbRepoBookRepository;
    }

    // TODO: Allow passing key-values or remove
    public SyncRepo repoInstance(RepoType type, String url) {
        return dataRepository.getRepoInstance(13, type, url);
    }

    public Repo setupRepo(RepoType type, String url) {
        long id = dataRepository.createRepo(new RepoWithProps(new Repo(0, type, url)));

        return dataRepository.getRepo(id);
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
            Repo newRepo = new Repo(repo.getId(), repo.getType(), toUrl);
            dataRepository.updateRepo(new RepoWithProps(newRepo));
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

    public BookView setupBook(String name, String content, Repo link) {
        BookView bookView = null;
        try {
            bookView = loadBookFromContent(name, BookFormat.ORG, content, null);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        dataRepository.setLink(bookView.getBook().getId(), link);
        return bookView;
    }

    /**
     * Overwrites existing repoUrl / url combinations (due to table definition).
     */
    public void setupRook(Repo repo, String url, String content, String rev, long mtime) {
        VersionedRook vrook = new VersionedRook(
                repo.getId(), repo.getType(), Uri.parse(repo.getUrl()), Uri.parse(url), rev, mtime);

        dbRepoBookRepository.createBook(repo.getId(), vrook, content);

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

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
