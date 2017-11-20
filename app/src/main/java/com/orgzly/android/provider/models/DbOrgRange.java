package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.provider.BaseColumns;

public class DbOrgRange implements DbOrgRangeColumns, BaseColumns {
    public static final String TABLE = "org_ranges";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            STRING + " TEXT UNIQUE," +

            START_TIMESTAMP_ID + " INTEGER," +
            END_TIMESTAMP_ID + " INTEGER," +

            DIFFERENCE + " INTEGER)",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public static void toContentValues(ContentValues values, com.orgzly.org.datetime.OrgRange range, long startId, long endId) {
        values.put(START_TIMESTAMP_ID, startId);

        values.put(STRING, range.toString());

        if (endId != 0) {
            values.put(END_TIMESTAMP_ID, endId);
        } else {
            values.putNull(END_TIMESTAMP_ID);
        }
    }
}
