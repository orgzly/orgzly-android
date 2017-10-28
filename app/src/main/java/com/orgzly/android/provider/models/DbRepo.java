package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

/**
 * User-configured repositories.
 */
public class DbRepo implements DbRepoColumns, BaseColumns {
    public static final String TABLE = "repos";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            REPO_URL + " TEXT NOT NULL, " +
            IS_REPO_ACTIVE + " INTEGER DEFAULT 1, " +
            "UNIQUE (" + REPO_URL + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    /**
     * Inserts new URL or updates existing marking it as active.
     */
    public static long insert(SQLiteDatabase db, String url) {
        ContentValues values = new ContentValues();
        values.put(REPO_URL, url);
        values.put(IS_REPO_ACTIVE, 1);

        long id = DatabaseUtils.getId(
                db,
                TABLE,
                REPO_URL + "=?",
                new String[] { url });

        if (id > 0) {
            db.update(TABLE, values, _ID + "=" + id, null);
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
        values.put(IS_REPO_ACTIVE, 0);

        return db.update(TABLE, values, selection, selectionArgs);
    }

    public static int update(SQLiteDatabase db, ContentValues contentValues, String selection, String[] selectionArgs) {
        return db.update(TABLE, contentValues, selection, selectionArgs);
    }
}
