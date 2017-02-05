package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

import com.orgzly.android.util.UriUtils;

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
    public VersionedRook retrieveBook(Uri uri, File file) throws IOException {
        return client.download(repoUri, uri, file);
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        return client.upload(file, repoUri, fileName);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        Uri toUri = UriUtils.getUriForNewName(fromUri, name);
        return client.move(repoUri, fromUri, toUri);
    }

    @Override
    public void delete(Uri uri) throws IOException {
        client.delete(uri.getPath());
    }

    @Override
    public String toString() {
        return repoUri.toString();
    }
}
