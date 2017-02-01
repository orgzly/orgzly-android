package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 * Local books.
 */
public class DbBook {
    public static final String TABLE = "books";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            Columns.NAME + " UNIQUE," +
            Columns.TITLE + " TEXT," +
            Columns.MTIME + " INTEGER," +
            Columns.IS_DUMMY + " INTEGER DEFAULT 0," +
            Columns.IS_DELETED + " INTEGER DEFAULT 0," +
            Columns.PREFACE + " TEXT," +
            Columns.IS_INDENTED + " INTEGER DEFAULT 0," +
            Columns.USED_ENCODING + " TEXT," +
            Columns.DETECTED_ENCODING + " TEXT," +
            Columns.SELECTED_ENCODING + " TEXT," +
            Columns.SYNC_STATUS + " TEXT," +
            Columns.LAST_ACTION_TIMESTAMP + " INTEGER," +
            Columns.LAST_ACTION_TYPE + " INTEGER," +
            Columns.LAST_ACTION + " TEXT)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.NAME + " ON " + TABLE + "(" + Columns.NAME + ")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String NAME = "name";
        String TITLE = "title";
        String IS_INDENTED = "is_indented";
        String MTIME = "mtime";
        String IS_DUMMY = "is_dummy";
        String IS_DELETED = "is_deleted";
        String SYNC_STATUS = "sync_status";
        String LAST_ACTION = "last_action";
        String LAST_ACTION_TIMESTAMP = "last_action_timestamp";
        String LAST_ACTION_TYPE = "last_action_type";
        String PREFACE = "preface";
        String DETECTED_ENCODING = "detected_encoding";
        String USED_ENCODING = "used_encoding";
        String SELECTED_ENCODING = "selected_encoding";
    }

    public static class Column implements Columns, BaseColumns {}
}
