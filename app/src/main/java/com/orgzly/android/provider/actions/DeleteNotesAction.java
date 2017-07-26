package com.orgzly.android.provider.actions;


import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.provider.models.DbNote;

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

        db.execSQL("DELETE FROM " + DbNote.TABLE + " WHERE " + DbNote.IS_CUT + " = " + batchId);

        return result;
    }

    @Override
    public void undo() {
    }
}