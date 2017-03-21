package com.orgzly.android.provider.models;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

public class DbNoteAncestor extends Model {
    public static final String TABLE = "note_ancestors";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "note_id INTEGER," +
            "ancestor_note_id INTEGER)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "note_id" + " ON " + TABLE + "(" + "note_id" + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "ancestor_note_id" + " ON " + TABLE + "(" + "ancestor_note_id" + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String NOTE_ID = "note_id";
        String ANCESTOR_NOTE_ID = "ancestor_note_id";
    }

    public static class Column implements Columns, BaseColumns {}

    public Long noteId;
    public Long ancestorNoteId;

    public DbNoteAncestor(Long noteId, Long ancestorNoteId) {
        this.noteId = noteId;
        this.ancestorNoteId = ancestorNoteId;
    }

    public long save(SQLiteDatabase db) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                "note_id = " + noteId + " AND ancestor_note_id = " + ancestorNoteId, null);


        if (id == 0) {
            ContentValues values = new ContentValues();

            values.put("note_id", noteId);
            values.put("ancestor_note_id", ancestorNoteId);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
