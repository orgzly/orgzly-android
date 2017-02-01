package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbRook {
    public static final String TABLE = "rooks";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Columns.REPO_ID + " INTEGER," +
            Columns.ROOK_URL_ID + " INTEGER," +
            "UNIQUE (" + Columns.REPO_ID + "," + Columns.ROOK_URL_ID + "))"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String ROOK_URL_ID = "rook_url_id";
        String REPO_ID = "repo_id";
    }

    public static class Column implements Columns, BaseColumns {}
}
