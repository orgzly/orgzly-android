package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.BuildConfig;
import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;
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

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Moving note " + id + " (" + selectedNotePosition + ") from book " + bookId + " in direction " + direction);


        long targetNoteId = 0;
        Place place = null;

        if (direction == -1) { /* Moving up. */
            Cursor cursor = db.query(
                    DbNote.TABLE,
                    new String[] { DbNote.Column._ID, DbNote.Column.LFT, DbNote.Column.LEVEL },
                    DatabaseUtils.whereUncutBookNotes(bookId) + " AND " + DbNote.Column.RGT + " < " + selectedNotePosition.getLft(),
                    null, null, null,
                    DbNote.Column.RGT + " DESC");

            try {
                if (cursor.moveToFirst()) {

                    long prevId = cursor.getLong(0);
                    long prevLevel = cursor.getLong(2);

                    if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Moving note " + id + " up: prevId: " + prevId + " prevLevel: " + prevLevel);

                    if (prevLevel == selectedNotePosition.getLevel()) {
                        targetNoteId = prevId;
                        place = Place.ABOVE;
                    }
                }

            } finally {
                cursor.close();
            }


        } else { /* Moving down. */
            Cursor cursor = db.query(
                    DbNote.TABLE,
                    new String[] { DbNote.Column._ID, DbNote.Column.LFT, DbNote.Column.LEVEL },
                    DatabaseUtils.whereUncutBookNotes(bookId) + " AND " + DbNote.Column.LFT + " > " + selectedNotePosition.getRgt(),
                    null, null, null,
                    DbNote.Column.LFT);

            try {
                if (cursor.moveToFirst()) {
                    long prevId = cursor.getLong(0);
                    long prevLevel = cursor.getLong(2);

                    if (prevLevel == selectedNotePosition.getLevel()) {
                        targetNoteId = prevId;
                        place = Place.BELOW;
                    }
                }

            } finally {
                cursor.close();
            }
        }

        if (targetNoteId != 0) {
            ContentValues values;

            /* Cut note and all its descendants. */
            long batchId = System.currentTimeMillis();
            values = new ContentValues();
            values.put(DbNote.Column.IS_CUT, batchId);
            db.update(DbNote.TABLE, values, DatabaseUtils.whereDescendantsAndNote(bookId, selectedNotePosition.getLft(), selectedNotePosition.getRgt()), null);

            /* Paste. */
            values = new ContentValues();
            values.put(ProviderContract.Paste.Param.BATCH_ID, batchId);
            values.put(ProviderContract.Paste.Param.BOOK_ID, bookId);
            values.put(ProviderContract.Paste.Param.NOTE_ID, targetNoteId);
            values.put(ProviderContract.Paste.Param.SPOT, place.toString());
            new PasteNotesAction(values).run(db);

            DatabaseUtils.updateBookMtime(db, bookId);
        }

        return 0;
    }

    @Override
    public void undo() {

    }
}