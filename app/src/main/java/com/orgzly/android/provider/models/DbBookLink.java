package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbBookLink implements DbBookLinkColumns, BaseColumns {
    public static final String TABLE = "book_links";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            BOOK_ID + " INTEGER," +
            REPO_ID + " INTEGER," +
            ROOK_ID + " INTEGER," +
            "UNIQUE(" + BOOK_ID + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
