package com.orgzly.android.provider;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.android.provider.clients.FiltersClient;
import com.orgzly.android.provider.models.DbSearch;

import java.util.HashMap;
import java.util.Map;

class ProviderFilters {
    /**
     * Updates ContentValues with the position. Sets it to maximum existing + 1.
     */
    static void updateWithNextPosition(SQLiteDatabase db, ContentValues values) {
        Cursor cursor = db.query(
                DbSearch.TABLE,
                new String[] { "MAX(" + DbSearch.Column.POSITION + ")" },
                null,
                null,
                null,
                null,
                null);

        try {
            if (cursor.moveToFirst()) {
                int maxPosition = cursor.getInt(0);
                values.put(DbSearch.Column.POSITION, maxPosition + 1);
            }
        } finally {
            cursor.close();
        }
    }

    static int moveFilterUp(SQLiteDatabase db, long id) {
        return moveFilter(db, id, true);
    }

    static int moveFilterDown(SQLiteDatabase db, long id) {
        return moveFilter(db, id, false);
    }

    /**
     * Swaps position of target filter and the one above or below it.
     * Also fixes all positions which is required as default filters were both added with position 1.
     *
     * FIXME: Slow and horrible.
     */
    static int moveFilter(SQLiteDatabase db, long id, boolean up) {
        Map<Long, Integer> originalPositions = new HashMap<>();
        Map<Long, Integer> newPositions = new HashMap<>();

        Cursor cursor = queryAll(db);

        try {
            int i = 1;
            long lastId = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long thisId = cursor.getLong(0);
                int thisPos = cursor.getInt(1);

                originalPositions.put(thisId, thisPos);
                newPositions.put(thisId, i);

                if (up) {
                    if (thisId == id && lastId != 0) {
                        newPositions.put(thisId, i - 1);
                        newPositions.put(lastId, i);
                    }

                    lastId = thisId;
                    i++;

                } else {
                    if (lastId != 0) {
                        newPositions.put(thisId, i - 1);
                        newPositions.put(lastId, i);
                        lastId = 0;
                    }

                    if (thisId == id) {
                        lastId = thisId;
                    }

                    i++;
                }
            }
        } finally {
            cursor.close();
        }

        updateChangedPositions(db, originalPositions, newPositions);

        db.setTransactionSuccessful();

        return 1;
    }

    private static Cursor queryAll(SQLiteDatabase db) {
        return db.query(
                DbSearch.TABLE,
                new String[] {
                        DbSearch.Column._ID,
                        DbSearch.Column.POSITION
                },
                null,
                null,
                null,
                null,
                FiltersClient.SORT_ORDER);
    }

    private static void updateChangedPositions(SQLiteDatabase db, Map<Long, Integer> originalPositions, Map<Long, Integer> newPositions) {
        for (long thisId: newPositions.keySet()) {
            if (originalPositions.get(thisId).intValue() != newPositions.get(thisId).intValue()) {
                db.execSQL("UPDATE " + DbSearch.TABLE + " SET " +
                           DbSearch.Column.POSITION + " = " + newPositions.get(thisId) +
                           " WHERE " + DbSearch.Column._ID + " = " + thisId);
            }
        }
    }
}
