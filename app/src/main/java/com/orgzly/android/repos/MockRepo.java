package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Wrapper around {@link com.orgzly.android.repos.LocalDbRepo}.
 *
 * Simulates slow network.
 *
 * TODO: Use LocalDirRepo instead, remove LocalDbRepo.
 */
public class MockRepo implements Repo {
    public static final String SCHEME = "mock";

    private static final long SLEEP_FOR_GET_BOOKS = 100;
    private static final long SLEEP_FOR_RETRIEVE_BOOK = 200;
    private static final long SLEEP_FOR_STORE_BOOK = 200;

    private LocalDbRepo localDbRepo;

    public MockRepo(Context context, String url) {
        localDbRepo = new LocalDbRepo(context, url);
    }

    @Override
    public boolean requiresConnection() {
        return false;
    }

    @Override
    public Uri getUri() {
        return localDbRepo.getUri();
    }

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        SystemClock.sleep(SLEEP_FOR_GET_BOOKS);
        return localDbRepo.getBooks();
    }

    @Override
    public VersionedRook retrieveBook(String fileName, File file) throws IOException {
        SystemClock.sleep(SLEEP_FOR_RETRIEVE_BOOK);
        return localDbRepo.retrieveBook(fileName, file);
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        SystemClock.sleep(SLEEP_FOR_STORE_BOOK);
        return localDbRepo.storeBook(file, fileName);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        SystemClock.sleep(SLEEP_FOR_STORE_BOOK);
        return localDbRepo.renameBook(fromUri, name);
    }

    @Override
    public void delete(Uri uri) throws IOException {
    }
}
