package com.orgzly.android.provider;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic database utilities, not specific to Orgzly database schema.
 *
 * TODO: Consider creating DSL for constructing queries.
 *
 * Check existing libraries - something like this (no code generators or such):
 * http://www.jooq.org/doc/latest/manual/getting-started/use-cases/jooq-as-a-standalone-sql-builder/
 */
public class GenericDatabaseUtils {
    public static void incrementFields(SQLiteDatabase db, String table, String selection, int count, String... fields) {
        if (fields.length == 0) {
            throw new IllegalArgumentException("No fields passed to incrementFields");
        }

        List<String> updates = new ArrayList<>();

        for (String field: fields) {
            updates.add(field + " = " + field + " + (" + count + ")");
        }

        String sql = "UPDATE " + table + " SET " + TextUtils.join(", ", updates) + " WHERE " + selection;

        db.execSQL(sql);
    }

    public static String whereNullOrZero(String field) {
        return "(" + field + " IS NULL OR " + field + " = 0 )";
    }

    public static String join(String table, String alias, String field1, String onTable, String field2) {
        return " LEFT OUTER JOIN " + table + " " + alias + " ON " + field(alias, field1) + " = " + field(onTable, field2) + " ";
    }

    public static String field(String table, String name) {
        return table + "." + name;
    }

    public static int getCount(Context context, Uri uri, String selection) {
        Cursor cursor = context.getContentResolver().query(
                uri, new String[] { "COUNT(*) AS count" }, selection, null, null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return 0;
            }

        } finally {
            cursor.close();
        }
    }
}
