package com.orgzly.android.provider.clients;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.repos.Repo;
import com.orgzly.android.repos.RepoFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReposClient {
    private static final String TAG = ReposClient.class.getName();

    public static Uri insert(Context context, String url) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Repos.Param.REPO_URL, url);

        return context.getContentResolver().insert(ProviderContract.Repos.ContentUri.repos(), values);
    }

    public static int delete(Context context, long id) {
        return context.getContentResolver().delete(
                ContentUris.withAppendedId(ProviderContract.Repos.ContentUri.repos(), id), null, null);
    }

    public static Map<String, Repo> getAll(Context context) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Repos.ContentUri.repos(),
                new String[] { ProviderContract.Repos.Param.REPO_URL },
                null,
                null,
                null);

        return getAll(context, cursor);
    }

    private static Map<String, Repo> getAll(Context context, Cursor cursor) {
        Map<String, Repo> result = new HashMap<>();

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String repoUrl = cursor.getString(0);

                Repo repo = RepoFactory.getFromUri(context, repoUrl);

                if (repo != null) {
                    result.put(repoUrl, repo);
                } else {
                    Log.e(TAG, "Unsupported repository URL\"" + repoUrl + "\"");
                }
            }
        } finally {
            cursor.close();
        }

        return result;
    }

    public static long getId(Context context, String url) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Repos.ContentUri.repos(),
                new String[] { ProviderContract.Repos.Param._ID },
                String.format("%s = ?", ProviderContract.Repos.Param.REPO_URL),
                new String[] { url },
                null);


        try {
            Log.i("Cursor count is", String.format("%s, %s", cursor.getCount(), url));
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return 0;
            }

        } finally {
            cursor.close();
        }

    }

    public static String getUrl(Context context, long id) {
        Uri uri = ContentUris.withAppendedId(ProviderContract.Repos.ContentUri.repos(), id);
        Cursor cursor = context.getContentResolver().query(
                uri, new String[] { ProviderContract.Repos.Param.REPO_URL }, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            } else {
                return null;
            }

        } finally {
            cursor.close();
        }
    }

    /**
     * Since old repository URL could be used, do not actually update the existing record,
     * but create a new one.
     */
    public static int updateUrl(Context mContext, long id, String url) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation
                        .newDelete(ContentUris.withAppendedId(ProviderContract.Repos.ContentUri.repos(), id))
                        .build());

        ops.add(ContentProviderOperation
                        .newInsert(ProviderContract.Repos.ContentUri.repos())
                        .withValue(ProviderContract.Repos.Param.REPO_URL, url)
                        .build());

        try {
            mContext.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return 1;
    }
}
