package com.orgzly.android.provider.actions;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.models.DbNote;

public class CycleVisibilityAction implements Action {
    private static final String TAG = CycleVisibilityAction.class.getName();

    private long bookId;

    public CycleVisibilityAction(long bookId) {
        this.bookId = bookId;
    }

    @Override
    public int run(SQLiteDatabase db) {
        boolean unfoldedNotesExist = unfoldedNotesExist(db, bookId);

        if (unfoldedNotesExist) {
            foldAllNotes(db, bookId);
        } else {
            unFoldAllNotes(db, bookId);
        }

        return 0;
    }

    /** Check if unfolded notes exist. */
    private boolean unfoldedNotesExist(SQLiteDatabase db, long bookId) {
        String selection = DatabaseUtils.whereUncutBookNotes(bookId) + " AND " +
                           GenericDatabaseUtils.whereNullOrZero(DbNote.IS_FOLDED);

        Cursor cursor = db.query(DbNote.TABLE, DatabaseUtils.PROJECTION_FOR_COUNT, selection, null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                if (cursor.getLong(0) > 0) {
                    return true;
                }
            }
            return false;
        } finally {
            cursor.close();
        }
    }

    private static void unFoldAllNotes(SQLiteDatabase db, long bookId) {
        ContentValues values = new ContentValues();
        values.put(DbNote.IS_FOLDED, 0);
        values.put(DbNote.FOLDED_UNDER_ID, 0);

        String selection = DbNote.BOOK_ID + " = " + bookId;
        db.update(DbNote.TABLE, values, selection, null);
    }

    public static void foldAllNotes(SQLiteDatabase db, long bookId) {
        String minLevel = "(SELECT min(" + DbNote.LEVEL + ") FROM " + DbNote.TABLE + " WHERE " + DatabaseUtils.whereUncutBookNotes(bookId) + ")";

        /* Fold under parent all except for top level notes. */
        db.execSQL("UPDATE " + DbNote.TABLE + " SET " + DbNote.FOLDED_UNDER_ID + " = " + DbNote.PARENT_ID +
                   " WHERE " + DatabaseUtils.whereUncutBookNotes(bookId) + " AND " + DbNote.LEVEL + " > " + minLevel);

        /* Set folded flag. */
        db.execSQL("UPDATE " + DbNote.TABLE + " SET " + DbNote.IS_FOLDED + " = 1" +
                   " WHERE " + DatabaseUtils.whereUncutBookNotes(bookId));
    }

    @Override
    public void undo() {

    }
}
