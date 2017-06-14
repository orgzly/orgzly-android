package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

public class DbPropertyName {
    public static final String TABLE = "property_names";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +

            "name TEXT UNIQUE)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "name" + " ON " + TABLE + "(" + "name" + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String NAME = "name";
    }

    public static class Column implements Columns, BaseColumns {}

    public static long getOrInsert(SQLiteDatabase db, String name) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                Column.NAME + " = ?",
                new String[] { name });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(Column.NAME, name);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
