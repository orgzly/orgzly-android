package com.orgzly.android.repos;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Remote source of books (such as Dropbox directory, SSH directory, etc.)
 */
public interface Repo {
    boolean requiresConnection();

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
     * Download latest available revision of the book and store its content to {@code File}.
     */
    VersionedRook retrieveBook(Rook rook, File file) throws IOException;

    /**
     * Uploads book storing it under given filename under repo's url.
     */
    VersionedRook storeBook(File file, String path) throws IOException;

    // TODO: Use Rooks to know uri's repo for creating VersionedRook
    VersionedRook moveBook(Uri from, Uri to) throws IOException;

    void delete(String path) throws IOException;

    String toString();
}
