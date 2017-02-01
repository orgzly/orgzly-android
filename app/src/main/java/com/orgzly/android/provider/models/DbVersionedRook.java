package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbVersionedRook {
    public static final String TABLE = "versioned_rooks";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Columns.ROOK_ID + " INTEGER," +
            Columns.ROOK_MTIME + " INTEGER," +
            Columns.ROOK_REVISION + ")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String ROOK_ID = "rook_id";
        String ROOK_REVISION = "rook_revision";
        String ROOK_MTIME = "rook_mtime";
    }
    
    public static class Column implements Columns, BaseColumns {}
}
