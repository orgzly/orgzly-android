package com.orgzly.android.repos;

import android.net.Uri;
import android.os.SystemClock;


import com.orgzly.android.data.DbRepoBookRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Wrapper around {@link DatabaseRepo}.
 *
 * Simulates slow network.
 *
 * TODO: Use {@link DirectoryRepo} instead, remove {@link DbRepoBookRepository}.
 */
public class MockRepo implements SyncRepo {
    public static final String SCHEME = "mock";

    private static final long SLEEP_FOR_GET_BOOKS = 100;
    private static final long SLEEP_FOR_RETRIEVE_BOOK = 200;
    private static final long SLEEP_FOR_STORE_BOOK = 200;
    private static final long SLEEP_FOR_DELETE_BOOK = 100;

    private DatabaseRepo databaseRepo;

    public MockRepo(DbRepoBookRepository dbRepo, String url) {
        databaseRepo = new DatabaseRepo(dbRepo, url);
    }

    @Override
    public boolean requiresConnection() {
        return false;
    }

    @Override
    public Uri getUri() {
        return databaseRepo.getUri();
    }

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        SystemClock.sleep(SLEEP_FOR_GET_BOOKS);
        return databaseRepo.getBooks();
    }

    @Override
    public VersionedRook retrieveBook(String fileName, File file) throws IOException {
        SystemClock.sleep(SLEEP_FOR_RETRIEVE_BOOK);
        return databaseRepo.retrieveBook(fileName, file);
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        SystemClock.sleep(SLEEP_FOR_STORE_BOOK);
        return databaseRepo.storeBook(file, fileName);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        SystemClock.sleep(SLEEP_FOR_STORE_BOOK);
        return databaseRepo.renameBook(fromUri, name);
    }

    @Override
    public void delete(Uri uri) throws IOException {
        SystemClock.sleep(SLEEP_FOR_DELETE_BOOK);
        databaseRepo.delete(uri);
    }
}
