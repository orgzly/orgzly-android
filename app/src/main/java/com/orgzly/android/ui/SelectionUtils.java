package com.orgzly.android.ui;

import android.database.Cursor;
import android.provider.BaseColumns;

import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;

import java.util.HashSet;
import java.util.Set;

public class SelectionUtils {
    private static final String TAG = SelectionUtils.class.getName();

    public static void removeNonExistingIdsFromSelection(Selection selection, Cursor cursor) {
        if (selection.getCount() > 0) {
            long t = System.currentTimeMillis();

            Set<Long> existingIds = new HashSet<>();
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
                existingIds.add(id);
            }

            Set<Long> nonExistingSelectedIds = new HashSet<>();
            for (long id: selection.getIds()) {
                if (!existingIds.contains(id)) {
                    nonExistingSelectedIds.add(id);
                }
            }

            selection.deselectAll(nonExistingSelectedIds);

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Removed "
                                + nonExistingSelectedIds.size()
                                + " non-existing selected ids in "
                                + (System.currentTimeMillis() - t));
        }
    }
}
