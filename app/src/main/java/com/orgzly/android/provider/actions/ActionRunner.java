package com.orgzly.android.provider.actions;

import android.database.sqlite.SQLiteDatabase;

import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;

/**
 * TODO: Encode action and save it to support undo.
 */
public class ActionRunner {
    private static final String TAG = ActionRunner.class.getName();

    public static int run(SQLiteDatabase db, Action action) {
        int result = -1;

        long t = System.currentTimeMillis();

        db.beginTransaction();

        try {
            result = action.run(db);

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, action.getClass() + ": " + (System.currentTimeMillis() - t) + "ms");

        return result;
    }
}
