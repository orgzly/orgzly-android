package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbCurrentVersionedRook implements DbCurrentVersionedRookColumns, BaseColumns {
    public static final String TABLE = "current_versioned_rooks";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            VERSIONED_ROOK_ID + " INTEGER)"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
