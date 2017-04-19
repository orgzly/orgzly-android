package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.provider.BaseColumns;

import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgDelay;
import com.orgzly.org.datetime.OrgInterval;
import com.orgzly.org.datetime.OrgRepeater;

import java.util.Calendar;

/**
 * <2017-04-16 Sun>
 * <2017-01-02 Mon 13:00>
 * <2017-04-16 Sun .+1d>
 * <2017-01-02 Mon 09:00 ++1d/2d>
 * <2017-04-16 Sun .+1d -0d>
 * <2006-11-02 Thu 20:00-22:00>
 */
public class DbOrgTimestamp {
    public static final String TABLE = "org_timestamp";

//    public static final int UNIT_SECOND = 101;
//    public static final int UNIT_MINUTE = 260;
    public static final int UNIT_HOUR   = 360;
    public static final int UNIT_DAY    = 424;
    public static final int UNIT_WEEK   = 507;
    public static final int UNIT_MONTH  = 604;
    public static final int UNIT_YEAR   = 712;

    public static final int REPEATER_TYPE_CUMULATE = 20;
    public static final int REPEATER_TYPE_CATCH_UP = 22;
    public static final int REPEATER_TYPE_RESTART = 12;

    public static final int DELAY_TYPE_ALL = 1;
    public static final int DELAY_TYPE_FIRST_ONLY = 2;

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID              + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            Columns.STRING               + " TEXT NOT NULL UNIQUE," +

            Columns.IS_ACTIVE            + " INTEGER NOT NULL," +

            Columns.YEAR                 + " INTEGER NOT NULL," +
            Columns.MONTH                + " INTEGER NOT NULL," +
            Columns.DAY                  + " INTEGER NOT NULL," +

            Columns.HOUR                 + " INTEGER," +
            Columns.MINUTE               + " INTEGER," +
            Columns.SECOND               + " INTEGER," +

            Columns.END_HOUR             + " INTEGER," +
            Columns.END_MINUTE           + " INTEGER," +
            Columns.END_SECOND           + " INTEGER," +

            Columns.REPEATER_TYPE        + " INTEGER," +
            Columns.REPEATER_VALUE       + " INTEGER," +
            Columns.REPEATER_UNIT        + " INTEGER," +

            Columns.HABIT_DEADLINE_VALUE + " INTEGER," +
            Columns.HABIT_DEADLINE_UNIT  + " INTEGER," +

            Columns.DELAY_TYPE           + " INTEGER," +
            Columns.DELAY_VALUE          + " INTEGER," +
            Columns.DELAY_UNIT           + " INTEGER," +

            Columns.TIMESTAMP            + " INTEGER," +
            Columns.END_TIMESTAMP        + " INTEGER)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.STRING + " ON " + TABLE + "(" + Columns.STRING + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.TIMESTAMP + " ON " + TABLE + "(" + Columns.TIMESTAMP + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.END_TIMESTAMP + " ON " + TABLE + "(" + Columns.END_TIMESTAMP + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public interface Columns {
        String IS_ACTIVE = "is_active";

        String STRING = "string";

        String YEAR = "year";
        String MONTH = "month";
        String DAY = "day";

        String HOUR = "hour";
        String MINUTE = "minute";
        String SECOND = "second";

        String END_HOUR = "end_hour";
        String END_MINUTE = "end_minute";
        String END_SECOND = "end_second";

        String REPEATER_TYPE = "repeater_type";
        String REPEATER_VALUE = "repeater_value";
        String REPEATER_UNIT = "repeater_unit";

        String HABIT_DEADLINE_VALUE = "habit_deadline_value";
        String HABIT_DEADLINE_UNIT = "habit_deadline_unit";

        String DELAY_TYPE = "delay_type";
        String DELAY_VALUE = "delay_value";
        String DELAY_UNIT = "delay_unit";

        /* Unix timestamp, when string value is used as if it were in the local time zone. */
        String TIMESTAMP = "timestamp";
        String END_TIMESTAMP = "end_timestamp";
    }

    public static class Column implements Columns, BaseColumns {}

    public static void toContentValues(ContentValues values, OrgDateTime orgDateTime) {
        values.put(Column.STRING, orgDateTime.toString());

        values.put(Column.IS_ACTIVE, orgDateTime.isActive() ? 1 : 0);

        values.put(Column.YEAR, orgDateTime.getCalendar().get(Calendar.YEAR));
        values.put(Column.MONTH, orgDateTime.getCalendar().get(Calendar.MONTH) + 1);
        values.put(Column.DAY, orgDateTime.getCalendar().get(Calendar.DAY_OF_MONTH));

        if (orgDateTime.hasTime()) {
            values.put(Column.HOUR, orgDateTime.getCalendar().get(Calendar.HOUR_OF_DAY));
            values.put(Column.MINUTE, orgDateTime.getCalendar().get(Calendar.MINUTE));
            values.put(Column.SECOND, orgDateTime.getCalendar().get(Calendar.SECOND));
        } else {
            values.putNull(Column.HOUR);
            values.putNull(Column.MINUTE);
            values.putNull(Column.SECOND);
        }

        values.put(Column.TIMESTAMP, orgDateTime.getCalendar().getTimeInMillis());

        if (orgDateTime.hasEndTime()) {
            values.put(Column.END_HOUR, orgDateTime.getEndCalendar().get(Calendar.HOUR_OF_DAY));
            values.put(Column.END_MINUTE, orgDateTime.getEndCalendar().get(Calendar.MINUTE));
            values.put(Column.END_SECOND, orgDateTime.getEndCalendar().get(Calendar.SECOND));
            values.put(Column.END_TIMESTAMP, orgDateTime.getEndCalendar().getTimeInMillis());
        } else {
            values.putNull(Column.END_HOUR);
            values.putNull(Column.END_MINUTE);
            values.putNull(Column.END_SECOND);
            values.putNull(Column.END_TIMESTAMP);
        }

        if (orgDateTime.hasRepeater()) {
            values.put(Column.REPEATER_TYPE, repeaterType(orgDateTime.getRepeater().getType()));
            values.put(Column.REPEATER_VALUE, orgDateTime.getRepeater().getValue());
            values.put(Column.REPEATER_UNIT, timeUnit(orgDateTime.getRepeater().getUnit()));

            if (orgDateTime.getRepeater().hasHabitDeadline()) {
                values.put(Column.HABIT_DEADLINE_VALUE, orgDateTime.getRepeater().getHabitDeadline().getValue());
                values.put(Column.HABIT_DEADLINE_UNIT, timeUnit(orgDateTime.getRepeater().getHabitDeadline().getUnit()));
            } else {
                values.putNull(Column.HABIT_DEADLINE_VALUE);
                values.putNull(Column.HABIT_DEADLINE_UNIT);
            }

        } else {
            values.putNull(Column.REPEATER_TYPE);
            values.putNull(Column.REPEATER_VALUE);
            values.putNull(Column.REPEATER_UNIT);
            values.putNull(Column.HABIT_DEADLINE_VALUE);
            values.putNull(Column.HABIT_DEADLINE_UNIT);
        }

        if (orgDateTime.hasDelay()) {
            values.put(Column.DELAY_TYPE, delayType(orgDateTime.getDelay().getType()));
            values.put(Column.DELAY_VALUE, orgDateTime.getDelay().getValue());
            values.put(Column.DELAY_UNIT, timeUnit(orgDateTime.getDelay().getUnit()));
        } else {
            values.putNull(Column.DELAY_TYPE);
            values.putNull(Column.DELAY_VALUE);
            values.putNull(Column.DELAY_UNIT);
        }
    }

    private static int repeaterType(OrgRepeater.Type type) {
        switch (type) {
            case CUMULATE:
                return REPEATER_TYPE_CUMULATE;
            case CATCH_UP:
                return REPEATER_TYPE_CATCH_UP;
            case RESTART:
                return REPEATER_TYPE_RESTART;
            default:
                return 0;
        }
    }

    private static int timeUnit(OrgInterval.Unit unit) {
        switch (unit) {
            case HOUR:
                return UNIT_HOUR;
            case DAY:
                return UNIT_DAY;
            case WEEK:
                return UNIT_WEEK;
            case MONTH:
                return UNIT_MONTH;
            case YEAR:
                return UNIT_YEAR;
            default:
                return 0;
        }
    }

    private static int delayType(OrgDelay.Type type) {
        switch (type) {
            case ALL:
                return DELAY_TYPE_ALL;
            case FIRST_ONLY:
                return DELAY_TYPE_FIRST_ONLY;
            default:
                return 0;
        }
    }
}
