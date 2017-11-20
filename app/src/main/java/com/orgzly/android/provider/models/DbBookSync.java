package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbBookSync implements DbBookSyncColumns, BaseColumns {
    public static final String TABLE = "book_syncs";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            BOOK_ID + " INTEGER," +
            BOOK_VERSIONED_ROOK_ID + " INTEGER," +
            "UNIQUE(" + BOOK_ID + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
