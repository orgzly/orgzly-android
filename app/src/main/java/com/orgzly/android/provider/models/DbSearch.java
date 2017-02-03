package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbSearch {
    public static final String TABLE = "searches";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Columns.NAME + " TEXT NOT NULL, " +
            Columns.QUERY + " TEXT NOT NULL, " +
            Columns.ICON + " INTEGER, " +
            Columns.GROUP + " TEXT, " +
            Columns.POSITION + " INTEGER DEFAULT 1, " +
            Columns.IS_SAVED + " INTEGER DEFAULT 0)",

            "INSERT INTO " + TABLE + " (" + Columns.NAME + ", " + Columns.QUERY + ") VALUES " +
            "(\"Scheduled\", \"s.today .i.done\")",

            "INSERT INTO " + TABLE + " (" + Columns.NAME + ", " + Columns.QUERY + ") VALUES " +
            "(\"To Do\", \"i.todo\")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String NAME = "name";
        String QUERY = "search";
        String ICON = "icon";
        String GROUP = "group_name";
        String POSITION = "position";
        String IS_SAVED = "is_saved";
    }

    public static class Column implements Columns, BaseColumns {}
}
