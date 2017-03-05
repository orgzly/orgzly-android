package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.ui.Place;


public class PasteNotesAction implements Action {
    private static final String TAG = PasteNotesAction.class.getName();

    private Place place;
    private long bookId;
    private long targetNoteId;
    private long batchId;

    public PasteNotesAction(ContentValues values) {
        place = Place.valueOf(values.getAsString(ProviderContract.Paste.Param.SPOT));
        bookId = values.getAsLong(ProviderContract.Paste.Param.BOOK_ID);
        targetNoteId = values.getAsLong(ProviderContract.Paste.Param.NOTE_ID);
        batchId = values.getAsLong(ProviderContract.Paste.Param.BATCH_ID);
    }

    @Override
    public int run(SQLiteDatabase db) {
        long batchMinLft;
        long batchMaxRgt;
        long batchMinLevel;
        long foldedUnder = 0;

        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] { "min(" + DbNote.Column.LFT + ")", "max(" + DbNote.Column.RGT + ")", "min(" + DbNote.Column.LEVEL + ")" },
                DbNote.Column.IS_CUT + " = " + batchId,
                null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                batchMinLft = cursor.getLong(0);
                batchMaxRgt = cursor.getLong(1);
                batchMinLevel = cursor.getLong(2);
            } else {
                return 0;
            }

        } finally {
            cursor.close();
        }

        NotePosition targetNotePosition = DbNote.getPosition(db, targetNoteId);

        long pastedLft, pastedLevel, pastedParentId;

        /* If target note is hidden, hide pasted under the same note. */
        if (targetNotePosition.getFoldedUnderId() != 0) {
            foldedUnder = targetNotePosition.getFoldedUnderId();
        }

        switch (place) {
            case ABOVE:
                pastedLft = targetNotePosition.getLft();
                pastedLevel = targetNotePosition.getLevel();
                pastedParentId = targetNotePosition.getParentId();
                break;

            case UNDER:
                NotePosition firstHighestLevelDescendant = getLastHighestLevelDescendant(db, targetNotePosition);

                if (firstHighestLevelDescendant != null) {
                    /* Insert batch after last descendant with highest level. */
                    pastedLft = firstHighestLevelDescendant.getRgt() + 1;
                    pastedLevel = firstHighestLevelDescendant.getLevel();

                } else {
                    /* Insert batch just under the target note. */
                    pastedLft = targetNotePosition.getLft() + 1;
                    pastedLevel = targetNotePosition.getLevel() + 1;
                }

                if (targetNotePosition.isFolded()) {
                    foldedUnder = targetNoteId;
                }

                pastedParentId = targetNoteId;

                break;

            case BELOW:
                pastedLft = targetNotePosition.getRgt() + 1;
                pastedLevel = targetNotePosition.getLevel();
                pastedParentId = targetNotePosition.getParentId();
                break;

            default:
                throw new IllegalArgumentException("Unsupported place for paste: " + place);
        }

        int positionsRequired = (int) (batchMaxRgt - batchMinLft + 1);
        long positionOffset = pastedLft - batchMinLft;
        long levelOffset = pastedLevel - batchMinLevel;


        String bookSelection = DatabaseUtils.whereUncutBookNotes(targetNotePosition.getBookId());

        /*
         * Make space for new notes incrementing lft and rgt.
         * FIXME: This could be slow.
         */

        GenericDatabaseUtils.incrementFields(db, DbNote.TABLE,
                bookSelection + " AND " + DbNote.Column.LFT + " >= " + pastedLft,
                positionsRequired, ProviderContract.Notes.UpdateParam.LFT);

        GenericDatabaseUtils.incrementFields(db, DbNote.TABLE,
                bookSelection + " AND " + DbNote.Column.RGT + " >= " + pastedLft,
                positionsRequired, ProviderContract.Notes.UpdateParam.RGT);

        /* Make sure batch has no no FOLDED_UNDER_ID IDs which do not belong to the batch itself. */
        db.execSQL("UPDATE " + DbNote.TABLE + " SET " + DbNote.Column.FOLDED_UNDER_ID + " = 0 WHERE " +
                   DbNote.Column.IS_CUT + " = " + batchId + " AND " +
                   DbNote.Column.FOLDED_UNDER_ID +
                   " NOT IN (SELECT " + DbNote.Column._ID +
                   " FROM " + DbNote.TABLE +
                   " WHERE " + DbNote.Column.IS_CUT + " = " + batchId + ")");

        /* Mark batch as folded. */
        if (foldedUnder != 0) {
            ContentValues values = new ContentValues();
            values.put(DbNote.Column.FOLDED_UNDER_ID, foldedUnder);
            String where = DbNote.Column.IS_CUT + " = " + batchId + " AND " + DbNote.Column.FOLDED_UNDER_ID + " = 0";
            db.update(DbNote.TABLE, values, where, null);
        }

        /* Update parent of the first note. */
        ContentValues values = new ContentValues();
        values.put(DbNote.Column.PARENT_ID, pastedParentId);
        db.update(DbNote.TABLE, values, DbNote.Column.IS_CUT + " = " + batchId + " AND " + DbNote.Column.LFT + " = " + batchMinLft, null);


        /* Move batch to the new position and make it visible. */
        String set = DbNote.Column.LFT + " = " + DbNote.Column.LFT + " + " + positionOffset + ", " +
                     DbNote.Column.RGT + " = " + DbNote.Column.RGT + " + " + positionOffset + ", " +
                     DbNote.Column.LEVEL + " = " + DbNote.Column.LEVEL + " + " + levelOffset + ", " +
                     DbNote.Column.IS_CUT + "= 0, " +
                     DbNote.Column.BOOK_ID + "= " + targetNotePosition.getBookId();
        String sql = "UPDATE " + DbNote.TABLE + " SET " + set + " WHERE " + DbNote.Column.IS_CUT + " = " + batchId;
        db.execSQL(sql);

        /* Update number of descendants for ancestors and the note itself. */
        String where = DatabaseUtils.whereAncestors(targetNotePosition.getBookId(), "" + targetNoteId) + " OR (" + DbNote.Column._ID + " = " + targetNoteId + ")";
        DatabaseUtils.updateDescendantsCount(db, where);

        /* Delete all other cut batches. */
        db.execSQL("DELETE FROM " + DbNote.TABLE + " WHERE " + DbNote.Column.IS_CUT + " != 0");

        DatabaseUtils.updateBookMtime(db, targetNotePosition.getBookId());

        return 0;
    }

    private NotePosition getLastHighestLevelDescendant(SQLiteDatabase db, NotePosition note) {
        NotePosition position = null;

        Cursor cursor = db.query(
                DbNote.TABLE,
                DbNote.POSITION_PROJECTION,
                DatabaseUtils.whereDescendants(note.getBookId(), note.getLft(), note.getRgt()),
                null, null, null, DbNote.Column.LEVEL + ", " + DbNote.Column.LFT + " DESC");

        try {
            if (cursor.moveToFirst()) {
                position = DbNote.positionFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        return position;
    }

    @Override
    public void undo() {

    }
}