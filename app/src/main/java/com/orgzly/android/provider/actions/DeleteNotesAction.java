package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;

/**
 * Same as Cut, but deletes the marked batch afterwords
 */
public class DeleteNotesAction implements Action {
    private long bookId;
    private String ids;

    public DeleteNotesAction(ContentValues values) {
        bookId = values.getAsLong(ProviderContract.Delete.Param.BOOK_ID);
        ids = values.getAsString(ProviderContract.Delete.Param.IDS);
    }

    @Override
    public int run(SQLiteDatabase db) {
        int result;

        long batchId = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(DbNote.Column.IS_CUT, batchId);

        String where = DatabaseUtils.whereDescendantsAndNotes(bookId, ids);
        result = db.update(DbNote.TABLE, values, where, null);

        String whereAncestors = DatabaseUtils.whereAncestors(bookId, ids);
        DatabaseUtils.updateDescendantsCount(db, whereAncestors);

        DatabaseUtils.updateBookMtime(db, bookId);

        db.execSQL("DELETE FROM notes WHERE is_cut = " + batchId);

        return result;
    }

    @Override
    public void undo() {
    }
}