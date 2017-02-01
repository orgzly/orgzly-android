package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;

/**
 * Marks notes as cut and returns their number.
 * Notes are cut by setting the flag to the current time in ms.
 */
public class CutNotesAction implements Action {
    private long bookId;
    private String ids;

    public CutNotesAction(ContentValues values) {
        bookId = values.getAsLong(ProviderContract.Cut.Param.BOOK_ID);
        ids = values.getAsString(ProviderContract.Cut.Param.IDS);
    }

    @Override
    public int run(SQLiteDatabase db) {
        int result;

        ContentValues values = new ContentValues();
        values.put(DbNote.Column.IS_CUT, System.currentTimeMillis());

        /* Mark as cut. */
        result = db.update(DbNote.TABLE, values, DatabaseUtils.whereDescendantsAndNotes(bookId, ids), null);

        /* Update number of descendants. */
        String whereAncestors = DatabaseUtils.whereAncestors(bookId, ids);
        DatabaseUtils.updateDescendantsCount(db, whereAncestors);

        DatabaseUtils.updateBookMtime(db, bookId);

        return result;
    }

    @Override
    public void undo() {
    }
}