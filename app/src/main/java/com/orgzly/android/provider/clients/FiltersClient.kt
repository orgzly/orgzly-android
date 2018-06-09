package com.orgzly.android.provider.clients

import android.content.*
import android.os.RemoteException
import android.support.v4.content.CursorLoader
import android.support.v4.util.LongSparseArray
import com.orgzly.android.AppIntent
import com.orgzly.android.filter.Filter
import com.orgzly.android.provider.GenericDatabaseUtils
import com.orgzly.android.provider.ProviderContract
import com.orgzly.android.provider.models.DbSearch
import com.orgzly.android.widgets.ListWidgetProvider
import java.util.ArrayList

object FiltersClient {
    @JvmStatic
    val SORT_ORDER = ProviderContract.Filters.Param.POSITION + ", " + ProviderContract.Filters.Param._ID

    @JvmStatic
    fun getCursorLoader(context: Context): CursorLoader =
            CursorLoader(context, ProviderContract.Filters.ContentUri.filters(), null, null, null, SORT_ORDER)

    @JvmStatic
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
    @JvmStatic
    fun getByNameIgnoreCase(context: Context, name: String): LongSparseArray<Filter> {
        val result = LongSparseArray<Filter>()

        val cursor = context.contentResolver.query(
                ProviderContract.Filters.ContentUri.filters(),
                arrayOf(ProviderContract.Filters.Param._ID, ProviderContract.Filters.Param.NAME, ProviderContract.Filters.Param.QUERY),
                ProviderContract.Filters.Param.NAME + " LIKE ?",
                arrayOf(name), null)

        cursor.use {
            GenericDatabaseUtils.forEachRow(cursor) {
                val id = cursor.getLong(0)

                val filter = Filter(
                        cursor.getString(1),
                        cursor.getString(2))

                result.put(id, filter)
            }
        }

        return result
    }

    /**
     * Deletes filter.
     *
     * @param id
     */
    @JvmStatic
    fun delete(context: Context, id: Long) {
        context.contentResolver.delete(ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id), null, null)

        updateWidgets(context)
    }

    @JvmStatic
    fun update(context: Context, id: Long, filter: Filter) {
        val values = ContentValues()

        values.put(ProviderContract.Filters.Param.NAME, filter.name)
        values.put(ProviderContract.Filters.Param.QUERY, filter.query)

        context.contentResolver.update(ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id), values, null, null)

        updateWidgets(context)
    }

    @JvmStatic
    fun create(context: Context, filter: Filter) {
        val values = ContentValues()

        values.put(ProviderContract.Filters.Param.NAME, filter.name)
        values.put(ProviderContract.Filters.Param.QUERY, filter.query)

        context.contentResolver.insert(ProviderContract.Filters.ContentUri.filters(), values)
    }

    @JvmStatic
    fun moveUp(context: Context, id: Long) {
        context.contentResolver.update(ProviderContract.Filters.ContentUri.filtersIdUp(id), null, null, null)
    }

    @JvmStatic
    fun moveDown(context: Context, id: Long) {
        context.contentResolver.update(ProviderContract.Filters.ContentUri.filtersIdDown(id), null, null, null)
    }

    private fun updateWidgets(context: Context) {
        val intent = Intent(context, ListWidgetProvider::class.java)
        intent.action = AppIntent.ACTION_UPDATE_LAYOUT_LIST_WIDGET
        context.sendBroadcast(intent)
    }

    fun forEach(context: Context, func: (filter: Filter) -> Unit) {
        val cursor = context.contentResolver.query(
                ProviderContract.Filters.ContentUri.filters(),
                arrayOf(ProviderContract.Filters.Param.NAME, ProviderContract.Filters.Param.QUERY),
                null,
                null,
                SORT_ORDER)

        cursor.use {
            GenericDatabaseUtils.forEachRow(cursor) {
                val name = cursor.getString(0)
                val query = cursor.getString(1)

                func(Filter(name, query))
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

        try {
            context.contentResolver.applyBatch(ProviderContract.AUTHORITY, ops)
        } catch (e: RemoteException) {
            e.printStackTrace()
            throw RuntimeException(e)
        } catch (e: OperationApplicationException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }

        updateWidgets(context)

        return filters.size
    }

}
