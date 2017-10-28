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
public class DbOrgTimestamp implements DbOrgTimestampColumns, BaseColumns {
    public static final String TABLE = "org_timestamps";

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

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            STRING + " TEXT NOT NULL UNIQUE," +

            IS_ACTIVE + " INTEGER NOT NULL," +

            YEAR + " INTEGER NOT NULL," +
            MONTH + " INTEGER NOT NULL," +
            DAY + " INTEGER NOT NULL," +

            HOUR + " INTEGER," +
            MINUTE + " INTEGER," +
            SECOND + " INTEGER," +

            END_HOUR + " INTEGER," +
            END_MINUTE + " INTEGER," +
            END_SECOND + " INTEGER," +

            REPEATER_TYPE + " INTEGER," +
            REPEATER_VALUE + " INTEGER," +
            REPEATER_UNIT + " INTEGER," +

            HABIT_DEADLINE_VALUE + " INTEGER," +
            HABIT_DEADLINE_UNIT + " INTEGER," +

            DELAY_TYPE + " INTEGER," +
            DELAY_VALUE + " INTEGER," +
            DELAY_UNIT + " INTEGER," +

            TIMESTAMP + " INTEGER," +
            END_TIMESTAMP + " INTEGER)",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + STRING + " ON " + TABLE + "(" + STRING + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + TIMESTAMP + " ON " + TABLE + "(" + TIMESTAMP + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + END_TIMESTAMP + " ON " + TABLE + "(" + END_TIMESTAMP + ")",
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;

    public static void toContentValues(ContentValues values, OrgDateTime orgDateTime) {
        values.put(STRING, orgDateTime.toString());

        values.put(IS_ACTIVE, orgDateTime.isActive() ? 1 : 0);

        values.put(YEAR, orgDateTime.getCalendar().get(Calendar.YEAR));
        values.put(MONTH, orgDateTime.getCalendar().get(Calendar.MONTH) + 1);
        values.put(DAY, orgDateTime.getCalendar().get(Calendar.DAY_OF_MONTH));

        if (orgDateTime.hasTime()) {
            values.put(HOUR, orgDateTime.getCalendar().get(Calendar.HOUR_OF_DAY));
            values.put(MINUTE, orgDateTime.getCalendar().get(Calendar.MINUTE));
            values.put(SECOND, orgDateTime.getCalendar().get(Calendar.SECOND));
        } else {
            values.putNull(HOUR);
            values.putNull(MINUTE);
            values.putNull(SECOND);
        }

        values.put(TIMESTAMP, orgDateTime.getCalendar().getTimeInMillis());

        if (orgDateTime.hasEndTime()) {
            values.put(END_HOUR, orgDateTime.getEndCalendar().get(Calendar.HOUR_OF_DAY));
            values.put(END_MINUTE, orgDateTime.getEndCalendar().get(Calendar.MINUTE));
            values.put(END_SECOND, orgDateTime.getEndCalendar().get(Calendar.SECOND));
            values.put(END_TIMESTAMP, orgDateTime.getEndCalendar().getTimeInMillis());
        } else {
            values.putNull(END_HOUR);
            values.putNull(END_MINUTE);
            values.putNull(END_SECOND);
            values.putNull(END_TIMESTAMP);
        }

        if (orgDateTime.hasRepeater()) {
            values.put(REPEATER_TYPE, repeaterType(orgDateTime.getRepeater().getType()));
            values.put(REPEATER_VALUE, orgDateTime.getRepeater().getValue());
            values.put(REPEATER_UNIT, timeUnit(orgDateTime.getRepeater().getUnit()));

            if (orgDateTime.getRepeater().hasHabitDeadline()) {
                values.put(HABIT_DEADLINE_VALUE, orgDateTime.getRepeater().getHabitDeadline().getValue());
                values.put(HABIT_DEADLINE_UNIT, timeUnit(orgDateTime.getRepeater().getHabitDeadline().getUnit()));
            } else {
                values.putNull(HABIT_DEADLINE_VALUE);
                values.putNull(HABIT_DEADLINE_UNIT);
            }

        } else {
            values.putNull(REPEATER_TYPE);
            values.putNull(REPEATER_VALUE);
            values.putNull(REPEATER_UNIT);
            values.putNull(HABIT_DEADLINE_VALUE);
            values.putNull(HABIT_DEADLINE_UNIT);
        }

        if (orgDateTime.hasDelay()) {
            values.put(DELAY_TYPE, delayType(orgDateTime.getDelay().getType()));
            values.put(DELAY_VALUE, orgDateTime.getDelay().getValue());
            values.put(DELAY_UNIT, timeUnit(orgDateTime.getDelay().getUnit()));
        } else {
            values.putNull(DELAY_TYPE);
            values.putNull(DELAY_VALUE);
            values.putNull(DELAY_UNIT);
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
