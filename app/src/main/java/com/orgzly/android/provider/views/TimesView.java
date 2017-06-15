package com.orgzly.android.provider.views;

public class TimesView {
    static public final String VIEW_NAME = "times_view";

    static public final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    static public final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +
            "  SELECT\n" +
            "  n._id as note_id,\n" +
            "  n.book_id as book_id,\n" +
            "  coalesce(b.title, b.name) as book_name,\n" +
            "  n.state as note_state,\n" +
            "  substr(n.title, 0, 30) as note_title,\n" +
            "  1 as time_type,\n" +
            "  t.timestamp as timestamp,\n" +
            "  t.string as org_timestamp_string,\n" +
            "  (t.hour IS NOT NULL AND t.hour != \"\") as has_time_part\n" +
            "  FROM org_ranges r\n" +
            "  JOIN org_timestamps t ON (r.start_timestamp_id = t._id)\n" +
            "  JOIN notes n ON (r._id = n.scheduled_range_id)\n" +
            "  JOIN books b ON (b._id = n.book_id)\n" +
            "  WHERE t.is_active = 1\n" +
            "UNION\n" +
            "  SELECT\n" +
            "  n._id as note_id,\n" +
            "  n.book_id as book_id,\n" +
            "  coalesce(b.title, b.name) as book_name,\n" +
            "  n.state as note_state,\n" +
            "  substr(n.title, 0, 30) as note_title,\n" +
            "  2 as time_type,\n" +
            "  t.timestamp as timestamp,\n" +
            "  t.string as org_timestamp_string,\n" +
            "  (t.hour IS NOT NULL AND t.hour != \"\") as has_time_part\n" +
            "  FROM org_ranges r\n" +
            "  JOIN org_timestamps t ON (r.start_timestamp_id = t._id)\n" +
            "  JOIN notes n ON (r._id = n.deadline_range_id)\n" +
            "  JOIN books b ON (b._id = n.book_id)\n" +
            "  WHERE t.is_active = 1\n" +
            "ORDER BY note_id";

    public interface Columns {
        String NOTE_ID = "note_id";
        String BOOK_NAME = "link_rook_url";
        String NOTE_STATE = "note_state";
        String NOTE_TITLE = "note_title";
        String TIME_TYPE = "time_type";
        String ORG_TIMESTAMP_STRING = "org_timestamp_string";
        String HAS_TIME_PART = "has_time_part";
    }
}
