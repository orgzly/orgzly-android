package com.orgzly.android.provider.actions;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.models.DbNote;


/**
 * Toggle folded state of the note.
 */
public class ToggleFoldedStateAction implements Action {
    private static final String TAG = ToggleFoldedStateAction.class.getName();

    private long noteId;

    public ToggleFoldedStateAction(long noteId) {
        this.noteId = noteId;
    }

    @Override
    public int run(SQLiteDatabase db) {
        boolean isFolded;
        long lft;
        long rgt;
        long bookId;

        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] {
                        DbNote.IS_FOLDED,
                        DbNote.LFT,
                        DbNote.RGT,
                        DbNote.BOOK_ID
                },
                DbNote._ID + " = " + noteId,
                null,
                null,
                null,
                null);

        try {
            if (cursor.moveToFirst()) {
                isFolded = cursor.getInt(0) == 1;
                lft = cursor.getLong(1);
                rgt = cursor.getLong(2);
                bookId = cursor.getLong(3);

            } else {
                Log.e(TAG, "Failed getting note " + noteId);
                return 0;
            }

        } finally {
            cursor.close();
        }

        ContentValues values;
        String selection;

        /* Toggle folded flag for the note. */
        values = new ContentValues();
        values.put(DbNote.IS_FOLDED, isFolded ? 0 : 1);
        selection = DbNote._ID + " = " + noteId;
        db.update(DbNote.TABLE, values, selection, null);

        /* Toggle visibility of descendants. */

        if (isFolded) {
            /* Unfold. */
            values = new ContentValues();
            values.put(DbNote.FOLDED_UNDER_ID, 0);

            /*
             * All descendants which are hidden because of this note being folded.
             */
            selection =
                    DatabaseUtils.whereDescendants(bookId, lft, rgt) + " AND " +
                    DbNote.FOLDED_UNDER_ID + " = " + noteId;

        } else {
            /* Fold. */
            values = new ContentValues();
            values.put(DbNote.FOLDED_UNDER_ID, noteId);

            /*
             * All descendants which are not already hidden
             * (because one of the ancestors being folded).
             */
            selection =
                    DatabaseUtils.whereDescendants(bookId, lft, rgt) + " AND " +
                    GenericDatabaseUtils.whereNullOrZero(DbNote.FOLDED_UNDER_ID);
        }

        return db.update(DbNote.TABLE, values, selection, null);
    }

    @Override
    public void undo() {

    }
}
