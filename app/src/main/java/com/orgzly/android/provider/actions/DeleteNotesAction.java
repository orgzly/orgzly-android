package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

/**
 * Same as Cut, but deletes marked batch afterwords.
 */
public class DeleteNotesAction extends CutNotesAction {
    public DeleteNotesAction(ContentValues values) {
        super(values);
    }

    @Override
    public int run(SQLiteDatabase db) {
        int result = super.run(db);

        db.execSQL("DELETE FROM notes WHERE is_cut = " + batchId);

        return result;
    }

    @Override
    public void undo() {
    }
}