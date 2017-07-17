package com.orgzly.android.provider.models;

public interface DbOrgTimestampColumns {
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
