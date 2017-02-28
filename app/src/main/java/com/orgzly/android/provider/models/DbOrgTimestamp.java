package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.provider.BaseColumns;

import com.orgzly.org.datetime.OrgDateTime;

/**
 *
 */
public class DbOrgTimestamp {
    public static final String TABLE = "org_timestamps";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            Columns.STRING + " TEXT UNIQUE," +

            Columns.IS_ACTIVE + " INTEGER," +

            Columns.REPEATER_TYPE + " TEXT," +
            Columns.REPEATER_VALUE + " INTEGER," +
            Columns.REPEATER_UNIT + " TEXT," +

            Columns.REPEATER_HABIT_DEADLINE_VALUE + " INTEGER," +
            Columns.REPEATER_HABIT_DEADLINE_UNIT + " TEXT," +

            Columns.DELAY_TYPE + " TEXT," +
            Columns.DELAY_VALUE + " INTEGER," +
            Columns.DELAY_UNIT + " TEXT," +

            Columns.TIMESTAMP + " INTEGER," +
            Columns.TIMESTAMP_END + " INTEGER)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.TIMESTAMP + " ON " + TABLE + "(" + Columns.TIMESTAMP + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.TIMESTAMP_END + " ON " + TABLE + "(" + Columns.TIMESTAMP_END + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String STRING = "string";

        String IS_ACTIVE = "is_active";

        String REPEATER_TYPE = "repeater_type";
        String REPEATER_VALUE = "repeater_value";
        String REPEATER_UNIT = "repeater_unit";

        String REPEATER_HABIT_DEADLINE_VALUE = "repeater_at_least_value";
        String REPEATER_HABIT_DEADLINE_UNIT = "repeater_at_least_unit";

        String DELAY_TYPE = "delay_type";
        String DELAY_VALUE = "delay_value";
        String DELAY_UNIT = "delay_unit";

        /* Unix timestamp, when string value is used as if it were in the local time zone. */
        String TIMESTAMP = "timestamp";
        String TIMESTAMP_END = "timestamp_end";
    }

    public static class Column implements Columns, BaseColumns {}

    public static void toContentValues(ContentValues values, OrgDateTime orgDateTime) {
        values.put(Column.STRING, orgDateTime.toString());

        values.put(Column.IS_ACTIVE, orgDateTime.isActive() ? 1 : 0);

        values.put(Column.TIMESTAMP, orgDateTime.getCalendar().getTimeInMillis());

        if (orgDateTime.hasEndTime()) {
            values.put(Column.TIMESTAMP_END, orgDateTime.getEndCalendar().getTimeInMillis());
        } else {
            values.putNull(Column.TIMESTAMP_END);
        }

        if (orgDateTime.hasRepeater()) {
            values.put(Column.REPEATER_TYPE, orgDateTime.getRepeater().getType().toString());
            values.put(Column.REPEATER_VALUE, orgDateTime.getRepeater().getValue());
            values.put(Column.REPEATER_UNIT, orgDateTime.getRepeater().getUnit().toString());

            if (orgDateTime.getRepeater().hasHabitDeadline()) {
                values.put(Column.REPEATER_HABIT_DEADLINE_VALUE, orgDateTime.getRepeater().getHabitDeadline().getValue());
                values.put(Column.REPEATER_HABIT_DEADLINE_UNIT, orgDateTime.getRepeater().getHabitDeadline().getUnit().toString());
            } else {
                values.putNull(Column.REPEATER_HABIT_DEADLINE_VALUE);
                values.putNull(Column.REPEATER_HABIT_DEADLINE_UNIT);
            }

        } else {
            values.putNull(Column.REPEATER_TYPE);
            values.putNull(Column.REPEATER_VALUE);
            values.putNull(Column.REPEATER_UNIT);
            values.putNull(Column.REPEATER_HABIT_DEADLINE_VALUE);
            values.putNull(Column.REPEATER_HABIT_DEADLINE_UNIT);
        }

        if (orgDateTime.hasDelay()) {
            values.put(Column.DELAY_TYPE, orgDateTime.getDelay().getType().toString());
            values.put(Column.DELAY_VALUE, orgDateTime.getDelay().getValue());
            values.put(Column.DELAY_UNIT, orgDateTime.getDelay().getUnit().toString());
        } else {
            values.putNull(Column.DELAY_TYPE);
            values.putNull(Column.DELAY_VALUE);
            values.putNull(Column.DELAY_UNIT);
        }
    }
}
