package com.orgzly.android.provider.actions;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.models.DbNote;

public class SparseTreeAction implements Action {
    private static final String TAG = SparseTreeAction.class.getName();

    public static final String ID = "id";

    private long bookId;
    private ContentValues values;

    public SparseTreeAction(long bookId, ContentValues values) {
        this.bookId = bookId;
        this.values = values;
    }

    @Override
    public int run(SQLiteDatabase db) {
        int result = 0;

        CycleVisibilityAction.foldAllNotes(db, bookId);

        if (values.containsKey(ID)) {
            long noteId = values.getAsLong(ID);

            String ancestorsIds = DatabaseUtils.ancestorsIds(db, bookId, noteId);

            if (ancestorsIds != null) {
                ContentValues v;

                v = new ContentValues();
                v.put(DbNote.IS_FOLDED, 0);
                result = db.update(DbNote.TABLE, v, DbNote._ID + " IN (" + ancestorsIds + ")", null);

                v = new ContentValues();
                v.put(DbNote.FOLDED_UNDER_ID, 0);
                db.update(DbNote.TABLE, v, DbNote.FOLDED_UNDER_ID + " IN (" + ancestorsIds + ")", null);
            }
        }

        return result;
    }

    @Override
    public void undo() {

    }
}
