package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

/**
 * URLs of remote books
 */
public class DbRookUrl {
    public static final String TABLE = "rook_urls";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Columns.ROOK_URL + " TEXT, " +
            "UNIQUE (" + Columns.ROOK_URL + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public static long getOrInsert(SQLiteDatabase db, String rookUrl) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                Column.ROOK_URL + "=?",
                new String[] { rookUrl });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(Column.ROOK_URL, rookUrl);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }

    public interface Columns {
        String ROOK_URL = "rook_url";
    }

    public static class Column implements Columns, BaseColumns {}
}
