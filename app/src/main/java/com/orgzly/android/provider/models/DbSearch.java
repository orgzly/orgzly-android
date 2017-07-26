package com.orgzly.android.provider.models;

import android.provider.BaseColumns;

/**
 *
 */
public class DbSearch implements DbSearchColumns, BaseColumns {
    public static final String TABLE = "searches";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            NAME + " TEXT NOT NULL, " +
            QUERY + " TEXT NOT NULL, " +
            ICON + " INTEGER, " +
            GROUP + " TEXT, " +
            POSITION + " INTEGER DEFAULT 1, " +
            IS_SAVED + " INTEGER DEFAULT 0)",

            "INSERT INTO " + TABLE + " (" + NAME + ", " + QUERY + ") VALUES " +
            "(\"Scheduled\", \"s.today .i.done\")",

            "INSERT INTO " + TABLE + " (" + NAME + ", " + QUERY + ") VALUES " +
            "(\"To Do\", \"i.todo\")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
