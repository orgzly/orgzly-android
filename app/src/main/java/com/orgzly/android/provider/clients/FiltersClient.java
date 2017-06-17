package com.orgzly.android.provider.clients;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;
import android.support.v4.util.LongSparseArray;
import com.orgzly.android.AppIntent;
import com.orgzly.android.Filter;
import com.orgzly.android.provider.ProviderContract;

public class FiltersClient {
    public static String SORT_ORDER =  ProviderContract.Filters.Param.POSITION + ", " + ProviderContract.Filters.Param._ID;

    public static CursorLoader getCursorLoader(Context context) {
        return new CursorLoader(context, ProviderContract.Filters.ContentUri.filters(), null, null, null, SORT_ORDER);
    }

    public static Filter get(Context context, long id) {
        Cursor cursor = context.getContentResolver().query(
                ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id),
                new String[] {
                        ProviderContract.Filters.Param.NAME,
                        ProviderContract.Filters.Param.QUERY
                },
                null,
                null,
                null);

        try {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String query = cursor.getString(1);
                return new Filter(name, query);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    /**
     * Get {@link Filter} by name (case insensitive).
     *
     * For some time, when updating or creating a new filter, app didn't check if there are filters
     * with the same name. So it's possible they exist. It's not defined which one is returned here.
     *
     * @return First filter that matches the name (case insensitive)
     */
    public static LongSparseArray<Filter> getByNameIgnoreCase(Context context, String name) {
        LongSparseArray<Filter> result = new LongSparseArray<>();

        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Filters.ContentUri.filters(),
                new String[] {
                        ProviderContract.Filters.Param._ID,
                        ProviderContract.Filters.Param.NAME,
                        ProviderContract.Filters.Param.QUERY
                },
                ProviderContract.Filters.Param.NAME + " LIKE ?",
                new String[] { name },
                null);

        if (cursor != null) {
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    long id = cursor.getLong(0);

                    Filter filter = new Filter(
                            cursor.getString(1),
                            cursor.getString(2));

                    result.put(id, filter);
                }
            } finally {
                cursor.close();
            }
        }

        return result;
    }

    /**
     * Deletes filter.
     *
     * @param id
     */
    public static void delete(Context context, long id) {
        context.getContentResolver().delete(ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id), null, null);

        updateWidgets(context);
    }

    public static void update(Context context, long id, Filter filter) {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.Filters.Param.NAME, filter.getName());
        values.put(ProviderContract.Filters.Param.QUERY, filter.getQuery());

        context.getContentResolver().update(ContentUris.withAppendedId(ProviderContract.Filters.ContentUri.filters(), id), values, null, null);

        updateWidgets(context);
    }

    public static void create(Context context, Filter filter) {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.Filters.Param.NAME, filter.getName());
        values.put(ProviderContract.Filters.Param.QUERY, filter.getQuery());

        context.getContentResolver().insert(ProviderContract.Filters.ContentUri.filters(), values);
    }

    public static void moveUp(Context context, long id) {
        context.getContentResolver().update(ProviderContract.Filters.ContentUri.filtersIdUp(id), null, null, null);
    }

    public static void moveDown(Context context, long id) {
        context.getContentResolver().update(ProviderContract.Filters.ContentUri.filtersIdDown(id), null, null, null);
    }

    private static void updateWidgets(Context context) {
        context.sendBroadcast(new Intent(AppIntent.ACTION_LIST_WIDGET_UPDATE_LAYOUT));
    }
}
