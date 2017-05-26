package com.orgzly.android.provider.models;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

public class DbNoteProperty {
    public static final String TABLE = "note_properties";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +

            "note_id INTEGER," +
            "position INTEGER," +
            "property_id INTEGER," +

            "UNIQUE(note_id, position, property_id))",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "note_id" + " ON " + TABLE + "(" + "note_id" + ")", // TODO: Create util method to construct this kind of string
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "position" + " ON " + TABLE + "(" + "position" + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "property_id" + " ON " + TABLE + "(" + "property_id" + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String POSITION = "position";
        String NOTE_ID = "note_id";
        String PROPERTY_ID = "property_id";
    }

    public static class Column implements Columns, BaseColumns {}

    public static long getOrInsert(SQLiteDatabase db, long noteId, int position, long propertyId) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                Column.NOTE_ID + " = " + noteId + " AND " +
                Column.POSITION + " = " + position + " AND " +
                Column.PROPERTY_ID + " = " + propertyId,
                null);


        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(Column.NOTE_ID, noteId);
            values.put(Column.POSITION, position);
            values.put(Column.PROPERTY_ID, propertyId);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
