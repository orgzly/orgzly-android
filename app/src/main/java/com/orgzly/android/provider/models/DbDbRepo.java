package com.orgzly.android.provider.models;

import android.provider.BaseColumns;


/**
 *
 */
public class DbDbRepo {
    public static final String TABLE = "db_repos";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Columns.REPO_URL + " TEXT NOT NULL, " +
            Columns.URL + " TEXT NOT NULL, " +
            Columns.REVISION + " TEXT, " +
            Columns.MTIME + " INTEGER, " +
            Columns.CONTENT + " TEXT, " +
            Columns.CREATED_AT + " INTEGER, " +
            "UNIQUE (" + Columns.REPO_URL + ", " + Columns.URL + ") ON CONFLICT REPLACE)"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String REPO_URL = "repo_url";
        String URL = "url";
        String CONTENT = "content";
        String REVISION = "revision";
        String MTIME = "mtime";
        String CREATED_AT = "created_at";
    }
}
