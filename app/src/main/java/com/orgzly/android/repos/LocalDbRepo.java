package com.orgzly.android.repos;

import android.content.Context;
import android.net.Uri;

import com.orgzly.android.provider.clients.LocalDbRepoClient;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Repo which stores all its files in a local database.
 * Used for testing by {@link com.orgzly.android.repos.MockRepo}.
 */
public class LocalDbRepo implements Repo {
    private final Uri repoUri;

    private Context mContext;

    public LocalDbRepo(Context context, String url) {
        mContext = context;
        repoUri = Uri.parse(url);
    }

    @Override
    public boolean requiresConnection() {
        return false;
    }

    @Override
    public Uri getUri() {
        return repoUri;
    }

    @Override
    public List<VersionedRook> getBooks() throws IOException {
        return LocalDbRepoClient.getAll(mContext, repoUri);
    }

    @Override
    public VersionedRook retrieveBook(Uri uri, File file) throws IOException {
        return LocalDbRepoClient.retrieveBook(mContext, repoUri, uri, file);
    }

    @Override
    public VersionedRook storeBook(File file, String fileName) throws IOException {
        String content = MiscUtils.readStringFromFile(file);

        String rev = "MockedRevision-" + System.currentTimeMillis();
        long mtime = System.currentTimeMillis();

        Uri uri = repoUri.buildUpon().appendPath(fileName).build();

        VersionedRook vrook = new VersionedRook(repoUri, uri, rev, mtime);

        return LocalDbRepoClient.insert(mContext, vrook, content);
    }

    @Override
    public VersionedRook renameBook(Uri fromUri, String name) throws IOException {
        Uri toUri = UriUtils.getUriForNewName(fromUri, name);
        return LocalDbRepoClient.renameBook(mContext, fromUri, toUri);
    }

    @Override
    public void delete(Uri uri) throws IOException {

    }

    @Override
    public String toString() {
        return repoUri.toString();
    }
}
