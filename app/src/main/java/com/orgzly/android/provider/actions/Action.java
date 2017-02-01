package com.orgzly.android.provider.actions;

import android.database.sqlite.SQLiteDatabase;

public interface Action {
    int run(SQLiteDatabase db);

    void undo();
}
