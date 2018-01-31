package com.orgzly.android.provider.clients;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.repos.MockRepo;
import com.orgzly.android.repos.Rook;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.android.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Used by tests.
 *
 * TODO: Remove, see {@link MockRepo}.
 */
public class LocalDbRepoClient {
    public static VersionedRook fromCursor(Cursor cursor) {
        Uri repoUri = Uri.parse(cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.REPO_URL)));
        Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.URL)));
        String revision = cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.REVISION));
        long mtime = cursor.getLong(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.MTIME));

        return new VersionedRook(repoUri, uri, revision, mtime);
    }

    public static void delete(Context context) {
        context.getContentResolver().delete(ProviderContract.LocalDbRepo.ContentUri.dbRepos(), null, null);
    }

    public static VersionedRook retrieveBook(Context context, Uri repoUri, Uri uri, File file) throws IOException {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.LocalDbRepo.ContentUri.dbRepos(),
                null,
                ProviderContract.LocalDbRepo.Param.URL + "=?",
                new String[] { uri.toString() },
                null);

        try {

            if (!cursor.moveToFirst()) {
                throw new IOException("Book " + uri + " not found in repo");
            }

            if (cursor.getCount() != 1) {
                throw new IOException("Found " + cursor.getCount() + " books matching name " + uri);
            }

            /* Get data from Cursor. */
            String content = cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.CONTENT));
            String revision = cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.REVISION));
            long mtime = cursor.getLong(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.MTIME));

            /* Write content to file. */
            MiscUtils.writeStringToFile(content, file);

            /* Return book. */
            return new VersionedRook(repoUri, uri, revision, mtime);

        } finally {
            cursor.close();
        }
    }

    /**
     * Select only those belonging to this repo's name.
     */
    public static List<VersionedRook> getAll(Context context, Uri repoUri) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.LocalDbRepo.ContentUri.dbRepos(),
                null,
                ProviderContract.LocalDbRepo.Param.REPO_URL + "=?",
                new String[] { repoUri.toString() },
                null);

        List<VersionedRook> result = new ArrayList<>();

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.URL)));
                String revision = cursor.getString(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.REVISION));
                long mtime = cursor.getLong(cursor.getColumnIndex(ProviderContract.LocalDbRepo.Param.MTIME));

                result.add(new VersionedRook(repoUri, uri, revision, mtime));
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    private static VersionedRook get(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.LocalDbRepo.ContentUri.dbRepos(),
                null,
                ProviderContract.LocalDbRepo.Param.URL + " = ?",
                new String[] { uri.toString() },
                null);

        try {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    public static VersionedRook insert(Context context, VersionedRook vrook, String content) throws IOException {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.LocalDbRepo.Param.REPO_URL, vrook.getRepoUri().toString());
        values.put(ProviderContract.LocalDbRepo.Param.URL, vrook.getUri().toString());
        values.put(ProviderContract.LocalDbRepo.Param.CONTENT, content);
        values.put(ProviderContract.LocalDbRepo.Param.REVISION, vrook.getRevision());
        values.put(ProviderContract.LocalDbRepo.Param.MTIME, vrook.getMtime());
        values.put(ProviderContract.LocalDbRepo.Param.CREATED_AT, System.currentTimeMillis());

        Uri uri = context.getContentResolver().insert(ProviderContract.LocalDbRepo.ContentUri.dbRepos(), values);

        if (uri == null) {
            throw new IOException("Inserting repo book to db failed");
        }

        return vrook;
    }

    public static VersionedRook renameBook(Context context, Uri from, Uri to) throws IOException {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.LocalDbRepo.Param.URL, to.toString());
        values.put(ProviderContract.LocalDbRepo.Param.MTIME, System.currentTimeMillis());
        values.put(ProviderContract.LocalDbRepo.Param.REVISION, "MockedRenamedRevision-" + System.currentTimeMillis());

        int updated = context.getContentResolver().update(
                ProviderContract.LocalDbRepo.ContentUri.dbRepos(),
                values,
                ProviderContract.LocalDbRepo.Param.URL + " = ?",
                new String[] { from.toString() }
        );

        if (updated != 1) {
            throw new IOException("Failed moving notebook from " + from + " to " + to);
        }

        return get(context, to);
    }
}
