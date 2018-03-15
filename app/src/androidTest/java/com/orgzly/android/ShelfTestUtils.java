package com.orgzly.android;

import android.content.Context;
import android.net.Uri;

import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.clients.LocalDbRepoClient;
import com.orgzly.android.provider.clients.ReposClient;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.util.MiscUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Utility methods used by tests.
 * Creating and checking books, rooks, encodings etc.
 */
public class ShelfTestUtils {
    private Context context;
    private Shelf shelf;

    public ShelfTestUtils(Context context, Shelf shelf) {
        this.context = context;

        this.shelf = shelf;
    }

    public Repo setupRepo(String url) {
        ReposClient.insert(context, url);

        return RepoFactory.getFromUri(context, url);
    }

    public void deleteRepo(String url) {
        long id = ReposClient.getId(context, url);
        ReposClient.delete(context, id);
    }

    public void renameRepo(String fromUrl, String toUrl) {
        long id = ReposClient.getId(context, fromUrl);
        if (id > 0) {
            ReposClient.updateUrl(context, id, toUrl);
        } else {
            throw new IllegalStateException("Repo " + fromUrl + " does not exist");
        }
    }

    public Book setupBook(String name, String content) {
        Book book = null;

        try {
            book = loadBookFromContent(name, BookName.Format.ORG, content, null);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        return book;
    }

    public Book setupBook(String name, String content, String linkRepoUrl) {
        Book book = null;

        try {
            book = loadBookFromContent(name, BookName.Format.ORG, content, null);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        BooksClient.setLink(context, book.getId(), linkRepoUrl);

        return book;
    }

    public void setBookLink(long bookId, String repoUrl) {
        BooksClient.setLink(context, bookId, repoUrl);
    }

    /**
     * Overwrites existing repoUrl / url combinations (due to table definition).
     */
    public void setupRook(String repoUrl, String url, String content, String rev, long mtime) {
        try {
            VersionedRook vrook = new VersionedRook(Uri.parse(repoUrl), Uri.parse(url), rev, mtime);
            LocalDbRepoClient.insert(context, vrook, content);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }

        // RemoteBookRevision remoteBookRevision = new RemoteBookRevision(repoUrl, url, rev, mtime);
        // RemoteBooksHelper.updateOrInsert(testContext, remoteBookRevision);
    }

    public void assertBook(String name, String expectedContent) {
        assertEquals(expectedContent, getBookContent(name));
    }

    private String getBookContent(String name) {
        try {
            return shelf.getBookContent(name, BookName.Format.ORG);
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
    private Book loadBookFromContent(String name, BookName.Format format, String content, VersionedRook vrook) throws IOException {
        /* Save content to temporary file. */
        File tmpFile = shelf.getTempBookFile();
        MiscUtils.writeStringToFile(content, tmpFile);

        try {
            return shelf.loadBookFromFile(name, format, tmpFile, vrook);

        } finally {
            /* Delete temporary file. */
            tmpFile.delete();
        }
    }
}
