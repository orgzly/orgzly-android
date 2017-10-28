package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import com.orgzly.android.provider.DatabaseUtils;

public class DbProperty implements DbPropertyColumns, BaseColumns {
    public static final String TABLE = "properties";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +

            "name_id INTEGER," +
            "value_id INTEGER," +

            "UNIQUE(name_id, value_id))",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "name_id" + " ON " + TABLE + "(" + "name_id" + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + "value_id" + " ON " + TABLE + "(" + "value_id" + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public static long getOrInsert(SQLiteDatabase db, long nameId, long valueId) {
        long id = DatabaseUtils.getId(
                db,
                TABLE,
                "name_id = " + nameId + " AND value_id = " + valueId, null);

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put("name_id", nameId);
            values.put("value_id", valueId);

            id = db.insertOrThrow(TABLE, null, values);
        }

        return id;
    }
}
