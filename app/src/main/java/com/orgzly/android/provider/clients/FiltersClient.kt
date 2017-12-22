package com.orgzly.android.provider.clients

import android.content.*
import android.os.RemoteException
import android.support.v4.content.CursorLoader
import android.support.v4.util.LongSparseArray
import com.orgzly.android.AppIntent
import com.orgzly.android.filter.Filter
import com.orgzly.android.provider.ProviderContract
import com.orgzly.android.provider.models.DbSearch
import java.util.ArrayList

object FiltersClient {
    val SORT_ORDER = ProviderContract.Filters.Param.POSITION + ", " + ProviderContract.Filters.Param._ID

    fun getCursorLoader(context: Context): CursorLoader =
            CursorLoader(context, ProviderContract.Filters.ContentUri.filters(), null, null, null, SORT_ORDER)

    operator fun get(context: Context, id: Long): Filter? {
        val cursor = context.contentResolver.query(
                ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id),
                arrayOf(ProviderContract.Filters.Param.NAME, ProviderContract.Filters.Param.QUERY), null, null, null)

        cursor?.use {
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                val query = cursor.getString(1)
                return Filter(name, query)
            }
        }

        return null
    }

    /**
     * Get [Filter] by name (case insensitive).
     *
     * For some time, when updating or creating a new filter, app didn't check if there are filters
     * with the same name. So it's possible they exist. It's not defined which one is returned here.
     *
     * @return First filter that matches the name (case insensitive)
     */
    fun getByNameIgnoreCase(context: Context, name: String): LongSparseArray<Filter> {
        val result = LongSparseArray<Filter>()

        val cursor = context.contentResolver.query(
                ProviderContract.Filters.ContentUri.filters(),
                arrayOf(ProviderContract.Filters.Param._ID, ProviderContract.Filters.Param.NAME, ProviderContract.Filters.Param.QUERY),
                ProviderContract.Filters.Param.NAME + " LIKE ?",
                arrayOf(name), null)

        cursor?.use {
            cursor.moveToFirst()

            while (!cursor.isAfterLast) {
                val id = cursor.getLong(0)

                val filter = Filter(
                        cursor.getString(1),
                        cursor.getString(2))

                result.put(id, filter)
                cursor.moveToNext()
            }
        }

        return result
    }

    /**
     * Deletes filter.
     *
     * @param id
     */
    fun delete(context: Context, id: Long) {
        context.contentResolver.delete(ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id), null, null)

        updateWidgets(context)
    }

    fun update(context: Context, id: Long, filter: Filter) {
        val values = ContentValues()

        values.put(ProviderContract.Filters.Param.NAME, filter.name)
        values.put(ProviderContract.Filters.Param.QUERY, filter.query)

        context.contentResolver.update(ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id), values, null, null)

        updateWidgets(context)
    }

    fun create(context: Context, filter: Filter) {
        val values = ContentValues()

        values.put(ProviderContract.Filters.Param.NAME, filter.name)
        values.put(ProviderContract.Filters.Param.QUERY, filter.query)

        context.contentResolver.insert(ProviderContract.Filters.ContentUri.filters(), values)
    }

    fun moveUp(context: Context, id: Long) {
        context.contentResolver.update(ProviderContract.Filters.ContentUri.filtersIdUp(id), null, null, null)
    }

    fun moveDown(context: Context, id: Long) {
        context.contentResolver.update(ProviderContract.Filters.ContentUri.filtersIdDown(id), null, null, null)
    }

    private fun updateWidgets(context: Context) {
        context.sendBroadcast(Intent(AppIntent.ACTION_UPDATE_LAYOUT_LIST_WIDGET))
    }

    fun forEach(context: Context, func: (filter: Filter) -> Unit) {
        val cursor = context.contentResolver.query(
                ProviderContract.Filters.ContentUri.filters(),
                arrayOf(ProviderContract.Filters.Param.NAME, ProviderContract.Filters.Param.QUERY),
                null,
                null,
                SORT_ORDER)

        cursor?.use {
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                val name = cursor.getString(0)
                val query = cursor.getString(1)

                func(Filter(name, query))

                cursor.moveToNext()
            }
        }
    }

    fun replaceAll(context: Context, filters: List<Filter>): Int {
        val uri = ProviderContract.Filters.ContentUri.filters()

        val ops = ArrayList<ContentProviderOperation>()

        /* Delete all. */
        ops.add(ContentProviderOperation
                .newDelete(uri)
                .build()
        )

        filters.forEach {
            val values = ContentValues()

            values.put(DbSearch.NAME, it.name)
            values.put(DbSearch.QUERY, it.query)

            ops.add(ContentProviderOperation.newInsert(uri).withValues(values).build())
        }

        val result: Array<ContentProviderResult>

        try {
            result = context.contentResolver.applyBatch(ProviderContract.AUTHORITY, ops)
        } catch (e: RemoteException) {
            e.printStackTrace()
            throw RuntimeException(e)
        } catch (e: OperationApplicationException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }

        return result[0].count
    }

}
