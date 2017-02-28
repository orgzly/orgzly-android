package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.provider.BaseColumns;

public class DbOrgRange {
    public static final String TABLE = "org_ranges";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            Column._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            Column.STRING + " TEXT UNIQUE," +

            Column.START_TIMESTAMP_ID + " INTEGER," +
            Column.END_TIMESTAMP_ID + " INTEGER," +

            Column.DIFFERENCE + " INTEGER)",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    interface Columns {
        String DIFFERENCE = "difference";
        String END_TIMESTAMP_ID = "end_timestamp_id";
        String START_TIMESTAMP_ID = "start_timestamp_id";
        String STRING = "string";
    }

    public static class Column implements Columns, BaseColumns {}

    public static void toContentValues(ContentValues values, com.orgzly.org.datetime.OrgRange range, long startId, long endId) {
        values.put(Column.START_TIMESTAMP_ID, startId);

        values.put(Column.STRING, range.toString());

        if (endId != 0) {
            values.put(Column.END_TIMESTAMP_ID, endId);
        } else {
            values.putNull(Column.END_TIMESTAMP_ID);
        }
    }
}
