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
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.LogUtils;

public class MoveNotesAction implements Action {
    private static final String TAG = MoveNotesAction.class.getName();

    private long bookId;
    private Long id;
    private int direction;

    public MoveNotesAction(ContentValues values) {
        bookId = values.getAsLong(ProviderContract.Move.Param.BOOK_ID);
        id = values.getAsLong(ProviderContract.Move.Param.IDS);
        direction = values.getAsInteger(ProviderContract.Move.Param.DIRECTION);
    }

    @Override
    public int run(SQLiteDatabase db) {
        NotePosition selectedNotePosition = DbNote.getPosition(db, id);

        NotePlace notePlace = null;

        if (direction == -1) { /* Move up - paste above previous sibling. */
            Cursor cursor = db.query(
                    DbNote.TABLE,
                    new String[] { DbNote._ID, DbNote.LEVEL },
                    DatabaseUtils.whereUncutBookNotes(bookId) + " AND " + DbNote.RGT + " < " + selectedNotePosition.getLft(),
                    null, null, null,
                    DbNote.RGT + " DESC");

            try {
                if (cursor.moveToFirst()) {
                    long prevNoteId = cursor.getLong(0);
                    long prevNoteLevel = cursor.getLong(1);

                    if (prevNoteLevel == selectedNotePosition.getLevel()) {
                        notePlace = new NotePlace(bookId, prevNoteId, Place.ABOVE);
                    }
                }

            } finally {
                cursor.close();
            }


        } else { /* Move down - paste below next sibling. */
            Cursor cursor = db.query(
                    DbNote.TABLE,
                    new String[] { DbNote._ID, DbNote.LEVEL },
                    DatabaseUtils.whereUncutBookNotes(bookId) + " AND " + DbNote.LFT + " > " + selectedNotePosition.getRgt(),
                    null, null, null,
                    DbNote.LFT);

            try {
                if (cursor.moveToFirst()) {
                    long nextNoteId = cursor.getLong(0);
                    long nextNoteLevel = cursor.getLong(1);

                    if (nextNoteLevel == selectedNotePosition.getLevel()) {
                        notePlace = new NotePlace(bookId, nextNoteId, Place.BELOW);
                    }
                }

            } finally {
                cursor.close();
            }
        }

        if (notePlace != null) {
            ContentValues values;

            /* Delete affected notes from ancestors table. */
            String w = "(SELECT " + DbNote._ID + " FROM " + DbNote.TABLE + " WHERE " + DatabaseUtils.whereDescendantsAndNote(bookId, selectedNotePosition.getLft(), selectedNotePosition.getRgt()) + ")";
            String sql = "DELETE FROM " + DbNoteAncestor.TABLE + " WHERE " + DbNoteAncestor.NOTE_ID + " IN " + w;
            if (BuildConfig.LOG_DEBUG) LogUtils.d("SQL", sql);
            db.execSQL(sql);

            /* Cut note and all its descendants. */
            long batchId = System.currentTimeMillis();
            values = new ContentValues();
            values.put(DbNote.IS_CUT, batchId);
            db.update(DbNote.TABLE, values, DatabaseUtils.whereDescendantsAndNote(bookId, selectedNotePosition.getLft(), selectedNotePosition.getRgt()), null);

            /* Paste. */
            values = new ContentValues();
            values.put(ProviderContract.Paste.Param.BATCH_ID, batchId);
            values.put(ProviderContract.Paste.Param.NOTE_ID, notePlace.getNoteId());
            values.put(ProviderContract.Paste.Param.SPOT, notePlace.getPlace().toString());
            new PasteNotesAction(values).run(db);

            DatabaseUtils.updateBookMtime(db, bookId);
        }

        return 0;
    }

    @Override
    public void undo() {

    }
}