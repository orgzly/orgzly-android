package com.orgzly.android.provider.clients;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.repos.VersionedRook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class CurrentRooksClient {
    private static void toContentValues(ContentValues values, VersionedRook vrook) {
        values.put(ProviderContract.CurrentRooks.Param.REPO_URL, vrook.getRepoUri().toString());
        values.put(ProviderContract.CurrentRooks.Param.ROOK_URL, vrook.getUri().toString());
        values.put(ProviderContract.CurrentRooks.Param.ROOK_REVISION, vrook.getRevision());
        values.put(ProviderContract.CurrentRooks.Param.ROOK_MTIME, vrook.getMtime());
    }

    private static VersionedRook fromCursor(Cursor cursor) {
        return new VersionedRook(
                Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.CurrentRooks.Param.REPO_URL))),
                Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.CurrentRooks.Param.ROOK_URL))),
                cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.CurrentRooks.Param.ROOK_REVISION)),
                cursor.getLong(cursor.getColumnIndexOrThrow(ProviderContract.CurrentRooks.Param.ROOK_MTIME)));
    }

    public static void set(Context context, List<VersionedRook> books) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        /* Delete all previous. */
        ops.add(ContentProviderOperation
                .newDelete(ProviderContract.CurrentRooks.ContentUri.currentRooks())
                .build());

        /* Insert each one. */
        for (VersionedRook book: books) {
            ContentValues values = new ContentValues();
            CurrentRooksClient.toContentValues(values, book);

            ops.add(ContentProviderOperation
                    .newInsert(ProviderContract.CurrentRooks.ContentUri.currentRooks())
                    .withValues(values)
                    .build());
        }

        try {
            context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, VersionedRook> getAll(Context context) {
        Map<String, VersionedRook> result = new HashMap<>();

        Cursor cursor = context.getContentResolver().query(
                ProviderContract.CurrentRooks.ContentUri.currentRooks(), null, null, null, null);

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                VersionedRook vrook = CurrentRooksClient.fromCursor(cursor);
                result.put(vrook.getUri().toString(), vrook);
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static VersionedRook get(Context context, String url) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.CurrentRooks.ContentUri.currentRooks(),
                null,
                ProviderContract.CurrentRooks.Param.ROOK_URL + "=?",
                new String[] { url },
                null);
        try {
            if (cursor.moveToFirst()) {
                return CurrentRooksClient.fromCursor(cursor);
            }

        } finally {
            cursor.close();
        }

        return null;
    }

    public static VersionedRook get(Context context, String repoUrl, String url) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.CurrentRooks.ContentUri.currentRooks(),
                null,
                String.format("%s=? AND %s=?", ProviderContract.CurrentRooks.Param.ROOK_URL,
                        ProviderContract.CurrentRooks.Param.REPO_URL),
                new String[] { url, repoUrl },
                null);
        try {
            if (cursor.moveToFirst()) {
                return CurrentRooksClient.fromCursor(cursor);
            }

        } finally {
            cursor.close();
        }

        return null;
    }
}
