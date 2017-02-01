package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbBookSync {
    public static final String TABLE = "book_syncs";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Columns.BOOK_ID + " INTEGER," +
            Columns.BOOK_VERSIONED_ROOK_ID + " INTEGER," +
            "UNIQUE(" + Columns.BOOK_ID + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String BOOK_ID = "book_id";
        String BOOK_VERSIONED_ROOK_ID = "book_versioned_rook_id";
    }

    public static class Column implements Columns, BaseColumns {}
}
