package com.orgzly.android.ui

import android.database.Cursor
import android.provider.BaseColumns

import com.orgzly.BuildConfig
import com.orgzly.android.provider.GenericDatabaseUtils
import com.orgzly.android.util.LogUtils

import java.util.HashSet

object SelectionUtils {
    private val TAG = SelectionUtils::class.java.name

    @JvmStatic
    fun removeNonExistingIdsFromSelection(selection: Selection, cursor: Cursor) {
        if (selection.count > 0) {
            val t = System.currentTimeMillis()

            val existingIds = HashSet<Long>()

            GenericDatabaseUtils.forEachRow(cursor) {
                val id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
                existingIds.add(id)
            }

            val nonExistingSelectedIds = selection.ids
                    .filterNot { existingIds.contains(it) }
                    .toSet()

            selection.deselectAll(nonExistingSelectedIds)

            if (BuildConfig.LOG_DEBUG)
                LogUtils.d(TAG, "Removed "
                        + nonExistingSelectedIds.size
                        + " non-existing selected ids in "
                        + (System.currentTimeMillis() - t))
        }
    }
}
