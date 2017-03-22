package com.orgzly.android.provider.models;


public class DbNoteAncestor extends Model {
    public static final String TABLE = "note_ancestors";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +

            "book_id INTEGER," +
            "note_id INTEGER," +
            "ancestor_note_id INTEGER)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "book_id" + " ON " + TABLE + "(" + "book_id" + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "note_id" + " ON " + TABLE + "(" + "note_id" + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "ancestor_note_id" + " ON " + TABLE + "(" + "ancestor_note_id" + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String BOOK_ID = "book_id";
        String NOTE_ID = "note_id";
        String ANCESTOR_NOTE_ID = "ancestor_note_id";
    }

    public static class Column implements Columns {}
}
