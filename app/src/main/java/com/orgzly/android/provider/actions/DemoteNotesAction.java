package com.orgzly.android.provider.actions;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.BuildConfig;
import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.LogUtils;

public class DemoteNotesAction implements Action {
    private long bookId;
    private String ids;

    public DemoteNotesAction(ContentValues values) {
        bookId = values.getAsLong(ProviderContract.Demote.Param.BOOK_ID);
        ids = values.getAsString(ProviderContract.Demote.Param.IDS);
    }

    @Override
    public int run(SQLiteDatabase db) {
        int result = demote(db);

        DatabaseUtils.updateBookMtime(db, bookId);

        return result;
    }

    private int demote(SQLiteDatabase db) {
        Cursor cursor;
        NotePosition note;
        long previousSiblingId;
        ContentValues values;


        /* Get note info. */
        cursor = db.query(DbNote.TABLE, null, DbNote._ID + " IN (" + ids + ")", null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                note = DbNote.positionFromCursor(cursor);
            } else {
                return 0;
            }
        } finally {
            cursor.close();
        }

        previousSiblingId = DatabaseUtils.getPreviousSiblingId(db, note);

        if (previousSiblingId <= 0) {
            return 0;
        }

        /* Delete affected notes from ancestors table. */
        String w = "(SELECT " + DbNote._ID + " FROM " + DbNote.TABLE + " WHERE " + DatabaseUtils.whereDescendantsAndNotes(bookId, ids) + ")";
        String sql = "DELETE FROM " + DbNoteAncestor.TABLE + " WHERE " + DbNoteAncestor.NOTE_ID + " IN " + w;
        if (BuildConfig.LOG_DEBUG) LogUtils.d("SQL", sql);
        db.execSQL(sql);

        /* Cut note and all its descendants. */
        long batchId = System.currentTimeMillis();
        values = new ContentValues();
        values.put(DbNote.IS_CUT, batchId);
        db.update(DbNote.TABLE, values, DatabaseUtils.whereDescendantsAndNote(bookId, note.getLft(), note.getRgt()), null);

        /* Paste under previous sibling. */
        values = new ContentValues();
        values.put(ProviderContract.Paste.Param.BATCH_ID, batchId);
        values.put(ProviderContract.Paste.Param.NOTE_ID, previousSiblingId);
        values.put(ProviderContract.Paste.Param.SPOT, Place.UNDER.toString());
        new PasteNotesAction(values).run(db);

        DatabaseUtils.updateBookMtime(db, bookId);

        return 1;
    }

    @Override
    public void undo() {

    }
}
