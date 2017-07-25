package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.ui.Place;


public class PasteNotesAction implements Action {
    private static final String TAG = PasteNotesAction.class.getName();

    private Place place;
    private long targetNoteId;
    private long batchId;

    public PasteNotesAction(ContentValues values) {
        place = Place.valueOf(values.getAsString(ProviderContract.Paste.Param.SPOT));
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
                new String[] { "min(" + DbNote.LFT + ")", "max(" + DbNote.RGT + ")", "min(" + DbNote.LEVEL + ")" },
                DbNote.IS_CUT + " = " + batchId,
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
                NotePosition lastHighestLevelDescendant = getLastHighestLevelDescendant(db, targetNotePosition);

                if (lastHighestLevelDescendant != null) {
                    /* Insert batch after last descendant with highest level. */
                    pastedLft = lastHighestLevelDescendant.getRgt() + 1;
                    pastedLevel = lastHighestLevelDescendant.getLevel();

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


        /*
         * Make space for new notes incrementing lft and rgt.
         * FIXME: This could be slow.
         */

        String bookSelection = DatabaseUtils.whereUncutBookNotes(targetNotePosition.getBookId());

        GenericDatabaseUtils.incrementFields(
                db,
                DbNote.TABLE,
                bookSelection + " AND " + DbNote.LFT + " >= " + pastedLft,
                positionsRequired,
                ProviderContract.Notes.UpdateParam.LFT);

        GenericDatabaseUtils.incrementFields(
                db,
                DbNote.TABLE,
                "(" + bookSelection + " AND " + DbNote.RGT + " >= " + pastedLft + ") OR " + DbNote.LEVEL + " = 0",
                positionsRequired,
                ProviderContract.Notes.UpdateParam.RGT);

        /* Make sure batch has no no FOLDED_UNDER_ID IDs which do not belong to the batch itself. */
        db.execSQL("UPDATE " + DbNote.TABLE + " SET " + DbNote.FOLDED_UNDER_ID + " = 0 WHERE " +
                   DbNote.IS_CUT + " = " + batchId + " AND " +
                   DbNote.FOLDED_UNDER_ID +
                   " NOT IN (SELECT " + DbNote._ID +
                   " FROM " + DbNote.TABLE +
                   " WHERE " + DbNote.IS_CUT + " = " + batchId + ")");

        /* Mark batch as folded. */
        if (foldedUnder != 0) {
            ContentValues values = new ContentValues();
            values.put(DbNote.FOLDED_UNDER_ID, foldedUnder);
            String where = DbNote.IS_CUT + " = " + batchId + " AND " + DbNote.FOLDED_UNDER_ID + " = 0";
            db.update(DbNote.TABLE, values, where, null);
        }

        /* Update parent of the root of the batch. */
        ContentValues values = new ContentValues();
        values.put(DbNote.PARENT_ID, pastedParentId);
        db.update(DbNote.TABLE, values, DbNote.IS_CUT + " = " + batchId + " AND " + DbNote.LFT + " = " + batchMinLft, null);


        /* Move batch to the new position. */
        String set = DbNote.LFT + " = " + DbNote.LFT + " + " + positionOffset + ", " +
                     DbNote.RGT + " = " + DbNote.RGT + " + " + positionOffset + ", " +
                     DbNote.LEVEL + " = " + DbNote.LEVEL + " + " + levelOffset + ", " +
                     DbNote.BOOK_ID + "= " + targetNotePosition.getBookId();
        String sql = "UPDATE " + DbNote.TABLE + " SET " + set + " WHERE " + DbNote.IS_CUT + " = " + batchId;
        db.execSQL(sql);

        /* Insert ancestors for all notes of the batch. */
        db.execSQL("INSERT INTO " + DbNoteAncestor.TABLE +
                   " (" + DbNoteAncestor.BOOK_ID + ", " + DbNoteAncestor.NOTE_ID + ", " + DbNoteAncestor.ANCESTOR_NOTE_ID + ") " +
                   "SELECT n." + DbNote.BOOK_ID + ", n." + DbNote._ID + ", a." + DbNote._ID + " FROM " + DbNote.TABLE + " n " +
                   " JOIN " + DbNote.TABLE + " a ON (n." + DbNote.BOOK_ID + " = a." + DbNote.BOOK_ID +
                   " AND a." + DbNote.LFT + " < n." + DbNote.LFT +
                   " AND n." + DbNote.RGT + " < a." + DbNote.RGT + ") " +
                   "WHERE n." + DbNote.IS_CUT + " = " + batchId + "  AND " +
                   "a." + DbNote.LEVEL + " > 0");

        /* Make the batch visible. */
        db.execSQL("UPDATE " + DbNote.TABLE + " SET " + DbNote.IS_CUT  + " = 0 WHERE " + DbNote.IS_CUT + " = " + batchId);

        /* Update number of descendants for ancestors and the note itself. */
        String where = DatabaseUtils.whereAncestorsAndNote(targetNotePosition.getBookId(), targetNoteId);
        DatabaseUtils.updateDescendantsCount(db, where);

        /* Delete other batches. */
        db.execSQL("DELETE FROM " + DbNote.TABLE + " WHERE " + DbNote.IS_CUT + " != 0");

        DatabaseUtils.updateBookMtime(db, targetNotePosition.getBookId());

        return 0;
    }

    private NotePosition getLastHighestLevelDescendant(SQLiteDatabase db, NotePosition note) {
        NotePosition position = null;

        Cursor cursor = db.query(
                DbNote.TABLE,
                DbNote.POSITION_PROJECTION,
                DatabaseUtils.whereDescendants(note.getBookId(), note.getLft(), note.getRgt()),
                null, null, null, DbNote.LEVEL + ", " + DbNote.LFT + " DESC");

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