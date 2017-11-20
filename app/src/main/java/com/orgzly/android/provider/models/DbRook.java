package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbRook implements DbRookColumns, BaseColumns {
    public static final String TABLE = "rooks";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            REPO_ID + " INTEGER," +
            ROOK_URL_ID + " INTEGER," +
            "UNIQUE (" + REPO_ID + "," + ROOK_URL_ID + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
