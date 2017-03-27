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

    public Long noteId;
    public Integer position;
    public DbProperty property;

    /**
     *
     * @param noteId
     * @param position Starts from 0
     * @param property
     */
    public DbNoteProperty(Long noteId, Integer position, DbProperty property) {
        this.noteId = noteId;
        this.position = position;
        this.property = property;
    }

    public long save(SQLiteDatabase db) {
        long propertyId = property.save(db);

        long id = DatabaseUtils.getId(
                db,
                TABLE,
                "note_id = " + noteId + " AND position = " + position + " AND property_id = " + propertyId, null);


        if (id == 0) {
            ContentValues values = new ContentValues();

            values.put("note_id", noteId);
            values.put("position", position);
            values.put("property_id", propertyId);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
