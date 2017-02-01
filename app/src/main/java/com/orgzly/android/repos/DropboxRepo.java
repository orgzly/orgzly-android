package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import java.util.List;

public class DropboxRepo implements Repo {
    public static final String SCHEME = "dropbox";

    private final Uri repoUri;
    private final DropboxClient client;

    public DropboxRepo(Context context, Uri uri) {
        this.repoUri = uri;
        this.client = new DropboxClient(context);
    }

    @Override
    public boolean requiresConnection() {
        return true;
    }

    @Override
    public Uri getUri() {
        return repoUri;
    }

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        return client.getBooks(repoUri);
    }

    @Override
    public VersionedRook retrieveBook(Rook rook, File file) throws IOException {
        return client.download(rook, file);
    }

    @Override
    public VersionedRook storeBook(File file, String path) throws IOException {
        return client.upload(file, repoUri, path);
    }

    @Override
    public VersionedRook moveBook(Uri from, Uri to) throws IOException {
        return client.move(repoUri, from, to);
    }

    @Override
    public void delete(String uriString) throws IOException {
        String path = Uri.parse(uriString).getPath();

        client.delete(path);
    }

    @Override
    public String toString() {
        return repoUri.toString();
    }
}
