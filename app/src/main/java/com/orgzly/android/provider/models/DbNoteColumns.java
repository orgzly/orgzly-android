package com.orgzly.android.provider.models;

public interface DbNoteColumns {
    String BOOK_ID = "book_id";
    String POSITION = "position";

    String CREATED_AT = "created_at";

    String LEVEL = "level";
    String TITLE = "title";
    String TAGS = "tags";
    String STATE = "state";
    String PRIORITY = "priority";

    String SCHEDULED_RANGE_ID = "scheduled_range_id";
    String DEADLINE_RANGE_ID = "deadline_range_id";
    String CLOSED_RANGE_ID = "closed_range_id";
    String CLOCK_RANGE_ID = "clock_range_id";

    String LFT = "is_visible";
    String RGT = "parent_position";
    String IS_FOLDED = "is_collapsed"; /** Toggleable flag. */
    String FOLDED_UNDER_ID = "is_under_collapsed"; /** Hidden due to ancestor being folded. */
    String PARENT_ID = "parent_id";
    String DESCENDANTS_COUNT = "has_children";

    String IS_CUT = "is_cut";

    String CONTENT = "content";
    String CONTENT_LINE_COUNT = "content_line_count";
}
