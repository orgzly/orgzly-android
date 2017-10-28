package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

/**
 * URLs of remote books
 */
public class DbRookUrl implements DbRookUrlColumns, BaseColumns {
    public static final String TABLE = "rook_urls";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ROOK_URL + " TEXT, " +
            "UNIQUE (" + ROOK_URL + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public static long getOrInsert(SQLiteDatabase db, String rookUrl) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                ROOK_URL + "=?",
                new String[] { rookUrl });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(ROOK_URL, rookUrl);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
