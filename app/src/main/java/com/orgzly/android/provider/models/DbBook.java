package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 * Local books.
 */
public class DbBook implements DbBookColumns, BaseColumns {
    public static final String TABLE = "books";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            NAME + " UNIQUE," +
            TITLE + " TEXT," +
            MTIME + " INTEGER," +
            IS_DUMMY + " INTEGER DEFAULT 0," +
            IS_DELETED + " INTEGER DEFAULT 0," +
            PREFACE + " TEXT," +
            IS_INDENTED + " INTEGER DEFAULT 0," +
            USED_ENCODING + " TEXT," +
            DETECTED_ENCODING + " TEXT," +
            SELECTED_ENCODING + " TEXT," +
            SYNC_STATUS + " TEXT," +
            LAST_ACTION_TIMESTAMP + " INTEGER," +
            LAST_ACTION_TYPE + " INTEGER," +
            LAST_ACTION + " TEXT)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + NAME + " ON " + TABLE + "(" + NAME + ")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
