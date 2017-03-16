package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

import java.util.Set;

/**
 * User-configured repositories.
 */
public class DbRepo {
    public static final String TABLE = "repos";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Columns.REPO_URL + " TEXT NOT NULL, " +
            Columns.IS_REPO_ACTIVE + " INTEGER DEFAULT 1, " +
            "UNIQUE (" + Columns.REPO_URL + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    /**
     * Inserts new URL or updates existing marking it as active.
     */
    public static long insert(SQLiteDatabase db, String url) {
        ContentValues values = new ContentValues();
        values.put(Column.REPO_URL, url);
        values.put(Column.IS_REPO_ACTIVE, 1);

        long id = DatabaseUtils.getId(
                db,
                TABLE,
                Column.REPO_URL + "=?",
                new String[] { url });

        if (id > 0) {
            db.update(TABLE, values, Column._ID + "=" + id, null);
        } else {
            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }

    /**
     * Delete repos by marking them as inactive.
     */
    public static int delete(SQLiteDatabase db, String selection, String[] selectionArgs) {
        ContentValues values = new ContentValues();
        values.put(Column.IS_REPO_ACTIVE, 0);

        return db.update(TABLE, values, selection, selectionArgs);
    }

    public static int update(SQLiteDatabase db, ContentValues contentValues, String selection, String[] selectionArgs) {
        return db.update(TABLE, contentValues, selection, selectionArgs);
    }

    public interface Columns {
        String REPO_URL = "repo_url";
        String IS_REPO_ACTIVE = "is_repo_active";
    }

    public static class Column implements Columns, BaseColumns {}
}
