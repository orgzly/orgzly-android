package com.orgzly.android.provider.models;

import android.provider.BaseColumns;


/**
 *
 */
public class DbDbRepo implements DbDbRepoColumns, BaseColumns {
    public static final String TABLE = "db_repos";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            REPO_URL + " TEXT NOT NULL, " +
            URL + " TEXT NOT NULL, " +
            REVISION + " TEXT, " +
            MTIME + " INTEGER, " +
            CONTENT + " TEXT, " +
            CREATED_AT + " INTEGER, " +
            "UNIQUE (" + REPO_URL + ", " + URL + ") ON CONFLICT REPLACE)"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
}
