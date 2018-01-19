package com.orgzly.android.provider.models;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

public class DbNoteContentTime implements DbNoteContentTimeColumns, BaseColumns {
    public static final String TABLE = "note_content_times";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            NOTE_ID + " INTEGER," +
            TIME_ID + " INTEGER," +

            "UNIQUE(" + NOTE_ID + "," + TIME_ID +"))",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + NOTE_ID + " ON " + TABLE + "(" + NOTE_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + TIME_ID + " ON " + TABLE + "(" + TIME_ID + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public static long getOrInsert(SQLiteDatabase db, long noteId, long timeId) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                NOTE_ID + " = " + noteId + " AND " +
                TIME_ID + " = " + timeId,
                null);


        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(NOTE_ID, noteId);
            values.put(TIME_ID, timeId);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
