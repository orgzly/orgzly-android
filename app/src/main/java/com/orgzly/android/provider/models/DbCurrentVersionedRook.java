package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbCurrentVersionedRook {
    public static final String TABLE = "current_versioned_rooks";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Columns.VERSIONED_ROOK_ID + " INTEGER)"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String VERSIONED_ROOK_ID = "versioned_rook_id";
    }

    public static class Column implements Columns, BaseColumns {}
}
