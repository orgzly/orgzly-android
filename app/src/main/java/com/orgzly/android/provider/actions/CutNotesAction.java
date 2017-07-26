package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.BuildConfig;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.util.LogUtils;

/**
 * Marks notes as cut and returns their number.
 * Notes are cut by setting the flag to the current time in ms.
 */
public class CutNotesAction implements Action {
    private long bookId;
    private String ids;

    protected long batchId;

    public CutNotesAction(ContentValues values) {
        bookId = values.getAsLong(ProviderContract.Cut.Param.BOOK_ID);
        ids = values.getAsString(ProviderContract.Cut.Param.IDS);
    }

    @Override
    public int run(SQLiteDatabase db) {
        int result;

        batchId = System.currentTimeMillis();

        /* Delete affected notes from ancestors table. */
        String w = "(SELECT " + DbNote._ID + " FROM " + DbNote.TABLE + " WHERE " + DatabaseUtils.whereDescendantsAndNotes(bookId, ids) + ")";
        String sql = "DELETE FROM " + DbNoteAncestor.TABLE + " WHERE " + DbNoteAncestor.NOTE_ID + " IN " + w;
        if (BuildConfig.LOG_DEBUG) LogUtils.d("SQL", sql);
        db.execSQL(sql);

        /* Mark as cut. */
        ContentValues values = new ContentValues();
        values.put(DbNote.IS_CUT, batchId);
        String where = DatabaseUtils.whereDescendantsAndNotes(bookId, ids);
        result = db.update(DbNote.TABLE, values, where, null);

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