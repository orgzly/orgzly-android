package com.orgzly.android.provider.views;

public interface DbNoteViewColumns {
    String BOOK_NAME = "book_name";

    String INHERITED_TAGS = "inherited_tags";

    String SCHEDULED_RANGE_STRING = "scheduled_range_string"; // rename to just scheduled string
    String SCHEDULED_TIME_STRING = "scheduled_time_string";
    String SCHEDULED_TIME_END_STRING = "scheduled_time_string";
    String SCHEDULED_TIME_TIMESTAMP = "scheduled_time_timestamp";

    String DEADLINE_RANGE_STRING = "deadline_range_string";
    String DEADLINE_TIME_STRING = "deadline_time_string";
    String DEADLINE_TIME_END_STRING = "deadline_time_end_string";
    String DEADLINE_TIME_TIMESTAMP = "deadline_time_timestamp";

    String CLOSED_RANGE_STRING = "closed_range_string";
    String CLOSED_TIME_STRING = "closed_time_string";
    String CLOSED_TIME_END_STRING = "closed_time_end_string";

    String CLOCK_RANGE_STRING = "clock_range_string";
    String CLOCK_TIME_STRING = "clock_time_string";
    String CLOCK_TIME_END_STRING = "clock_time_end_string";
}
