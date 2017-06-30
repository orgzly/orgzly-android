package com.orgzly.android.provider.views;

import android.provider.BaseColumns;

import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.provider.models.DbOrgRange;
import com.orgzly.android.provider.models.DbOrgTimestamp;

import static com.orgzly.android.provider.GenericDatabaseUtils.field;

/**
 * Notes with book name and times.
 */
public class NotesView {
    public static final String VIEW_NAME = "notes_view";

    static public final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    static public final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +

            "SELECT " + DbNote.TABLE + ".*, " +

            "group_concat(t_notes_with_inherited_tags." + DbNote.Column.TAGS + ",' ') AS " + Columns.INHERITED_TAGS + ", " +

            "t_scheduled_range." + DbOrgRange.Column.STRING + " AS " + Columns.SCHEDULED_RANGE_STRING + ", " +
            "t_scheduled_timestamps_start." + DbOrgTimestamp.Column.STRING + " AS " + Columns.SCHEDULED_TIME_STRING + ", " +
            "t_scheduled_timestamps_end." + DbOrgTimestamp.Column.STRING + " AS " + Columns.SCHEDULED_TIME_END_STRING + ", " +
            "t_scheduled_timestamps_start." + DbOrgTimestamp.Column.TIMESTAMP + " AS " + Columns.SCHEDULED_TIME_TIMESTAMP + ", " +

            "t_deadline_range." + DbOrgRange.Column.STRING + " AS " + Columns.DEADLINE_RANGE_STRING + ", " +
            "t_deadline_timestamps_start." + DbOrgTimestamp.Column.STRING + " AS " + Columns.DEADLINE_TIME_STRING + ", " +
            "t_deadline_timestamps_end." + DbOrgTimestamp.Column.STRING + " AS " + Columns.DEADLINE_TIME_END_STRING + ", " +
            "t_deadline_timestamps_start." + DbOrgTimestamp.Column.TIMESTAMP + " AS " + Columns.DEADLINE_TIME_TIMESTAMP + ", " +

            "t_closed_range." + DbOrgRange.Column.STRING + " AS " + Columns.CLOSED_RANGE_STRING + ", " +
            "t_closed_timestamps_start." + DbOrgTimestamp.Column.STRING + " AS " + Columns.CLOSED_TIME_STRING + ", " +
            "t_closed_timestamps_end." + DbOrgTimestamp.Column.STRING + " AS " + Columns.CLOSED_TIME_END_STRING + ", " +

            "t_clock_range." + DbOrgRange.Column.STRING + " AS " + Columns.CLOCK_RANGE_STRING + ", " +
            "t_clock_timestamps_start." + DbOrgTimestamp.Column.STRING + " AS " + Columns.CLOCK_TIME_STRING + ", " +
            "t_clock_timestamps_end." + DbOrgTimestamp.Column.STRING + " AS " + Columns.CLOCK_TIME_END_STRING + ", " +

            "t_books." + DbBook.Column.NAME + " AS " + Columns.BOOK_NAME + " " +

            "FROM " + DbNote.TABLE + " " +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_scheduled_range", DbOrgRange.Column._ID, DbNote.TABLE, DbNote.Column.SCHEDULED_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_scheduled_timestamps_start", DbOrgTimestamp.Column._ID, "t_scheduled_range", DbOrgRange.Column.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_scheduled_timestamps_end", DbOrgTimestamp.Column._ID, "t_scheduled_range", DbOrgRange.Column.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_deadline_range", DbOrgRange.Column._ID, DbNote.TABLE, DbNote.Column.DEADLINE_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_deadline_timestamps_start", DbOrgTimestamp.Column._ID, "t_deadline_range", DbOrgRange.Column.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_deadline_timestamps_end", DbOrgTimestamp.Column._ID, "t_deadline_range", DbOrgRange.Column.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_closed_range", DbOrgRange.Column._ID, DbNote.TABLE, DbNote.Column.CLOSED_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_closed_timestamps_start", DbOrgTimestamp.Column._ID, "t_closed_range", DbOrgRange.Column.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_closed_timestamps_end", DbOrgTimestamp.Column._ID, "t_closed_range", DbOrgRange.Column.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_clock_range", DbOrgRange.Column._ID, DbNote.TABLE, DbNote.Column.CLOCK_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_clock_timestamps_start", DbOrgTimestamp.Column._ID, "t_clock_range", DbOrgRange.Column.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_clock_timestamps_end", DbOrgTimestamp.Column._ID, "t_clock_range", DbOrgRange.Column.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbBook.TABLE, "t_books", DbBook.Column._ID, DbNote.TABLE, DbNote.Column.BOOK_ID) +

            GenericDatabaseUtils.join(DbNoteAncestor.TABLE, "t_note_ancestors", DbNoteAncestor.Column.NOTE_ID, DbNote.TABLE, DbNote.Column._ID) +
            GenericDatabaseUtils.join(DbNote.TABLE, "t_notes_with_inherited_tags", DbNote.Column._ID, "t_note_ancestors", DbNoteAncestor.Column.ANCESTOR_NOTE_ID) +

            " GROUP BY " + field(DbNote.TABLE, DbNote.Column._ID);

    public static class Columns implements DbNote.Columns, BaseColumns {
        public static String BOOK_NAME = "book_name";

        public static String INHERITED_TAGS = "inherited_tags";

        public static String SCHEDULED_RANGE_STRING = "scheduled_range_string"; // rename to just scheduled string
        public static String SCHEDULED_TIME_STRING = "scheduled_time_string";
        public static String SCHEDULED_TIME_END_STRING = "scheduled_time_string";
        public static String SCHEDULED_TIME_TIMESTAMP = "scheduled_time_timestamp";

        public static String DEADLINE_RANGE_STRING = "deadline_range_string";
        public static String DEADLINE_TIME_STRING = "deadline_time_string";
        public static String DEADLINE_TIME_END_STRING = "deadline_time_end_string";
        public static String DEADLINE_TIME_TIMESTAMP = "deadline_time_timestamp";

        public static String CLOSED_RANGE_STRING = "closed_range_string";
        public static String CLOSED_TIME_STRING = "closed_time_string";
        public static String CLOSED_TIME_END_STRING = "closed_time_end_string";

        public static String CLOCK_RANGE_STRING = "clock_range_string";
        public static String CLOCK_TIME_STRING = "clock_time_string";
        public static String CLOCK_TIME_END_STRING = "clock_time_end_string";
    }
}
