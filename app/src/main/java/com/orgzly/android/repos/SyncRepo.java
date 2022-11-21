package com.orgzly.android.repos;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Remote source of books (such as Dropbox directory, SSH directory, etc.)
 */
public interface SyncRepo {
    boolean isConnectionRequired();

    boolean isAutoSyncSupported();

    /**
     * Unique URL.
     */
    Uri getUri();

    /**
     * Retrieve the list of all available books.
     *
     * @return array of all available books
     * @throws IOException
     */
    List<VersionedRook> getBooks() throws IOException;

    /**
     * Download the latest available revision of the book and store its content to {@code File}.
     */
    VersionedRook retrieveBook(String fileName, File destination) throws IOException;

    /**
     * Uploads book storing it under given filename under repo's url.
     * @param file The contents of this file should be stored at the remote location/repo
     * @param fileName The contents ({@code file}) should be stored under this name
     */
    VersionedRook storeBook(File file, String fileName) throws IOException;

    /**
     * Uploads file storing it under directory (pathInRepo) under repo's url.
     * @param file The contents of this file should be stored at the remote location/repo
     * @param pathInRepo The "/" separated path within the remote location/repo, create it if it doesn't exist
     * @param fileName The contents ({@code file}) should be stored under this name
     * @return {@code VersionedRook}
     * @throws IOException
     */
    VersionedRook storeFile(File file, String pathInRepo, String fileName) throws IOException;

    VersionedRook renameBook(Uri from, String name) throws IOException;

    // VersionedRook moveBook(Uri from, Uri uri) throws IOException;

    void delete(Uri uri) throws IOException;

    String toString();
}
