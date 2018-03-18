package com.orgzly.android.provider.views;

import android.provider.BaseColumns;

import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.provider.models.DbNoteColumns;
import com.orgzly.android.provider.models.DbOrgRange;
import com.orgzly.android.provider.models.DbOrgTimestamp;

import static com.orgzly.android.provider.GenericDatabaseUtils.ms2StartOfDay;

/**
 * Notes with book name and times.
 */
public class DbNoteView implements DbNoteViewColumns, DbNoteColumns, BaseColumns  {
    public static final String VIEW_NAME = "notes_view";

    public static final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    public static final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +

            "SELECT " + DbNote.TABLE + ".*, " +

            "group_concat(t_notes_with_inherited_tags." + DbNote.TAGS + ",' ') AS " + INHERITED_TAGS + ", " +

            "t_scheduled_range." + DbOrgRange.STRING + " AS " + SCHEDULED_RANGE_STRING + ", " +
            "t_scheduled_timestamps_start." + DbOrgTimestamp.STRING + " AS " + SCHEDULED_TIME_STRING + ", " +
            "t_scheduled_timestamps_end." + DbOrgTimestamp.STRING + " AS " + SCHEDULED_TIME_END_STRING + ", " +
            "t_scheduled_timestamps_start." + DbOrgTimestamp.TIMESTAMP + " AS " + SCHEDULED_TIME_TIMESTAMP + ", " +
            ms2StartOfDay("t_scheduled_timestamps_start." + DbOrgTimestamp.TIMESTAMP) + " AS " + SCHEDULED_TIME_START_OF_DAY + ", " +
            "t_scheduled_timestamps_start." + DbOrgTimestamp.HOUR + " AS " + SCHEDULED_TIME_HOUR + ", " +

            "t_deadline_range." + DbOrgRange.STRING + " AS " + DEADLINE_RANGE_STRING + ", " +
            "t_deadline_timestamps_start." + DbOrgTimestamp.STRING + " AS " + DEADLINE_TIME_STRING + ", " +
            "t_deadline_timestamps_end." + DbOrgTimestamp.STRING + " AS " + DEADLINE_TIME_END_STRING + ", " +
            "t_deadline_timestamps_start." + DbOrgTimestamp.TIMESTAMP + " AS " + DEADLINE_TIME_TIMESTAMP + ", " +
            ms2StartOfDay("t_deadline_timestamps_start." + DbOrgTimestamp.TIMESTAMP) + " AS " + DEADLINE_TIME_START_OF_DAY + ", " +
            "t_deadline_timestamps_start." + DbOrgTimestamp.HOUR + " AS " + DEADLINE_TIME_HOUR + ", " +

            "t_closed_range." + DbOrgRange.STRING + " AS " + CLOSED_RANGE_STRING + ", " +
            "t_closed_timestamps_start." + DbOrgTimestamp.STRING + " AS " + CLOSED_TIME_STRING + ", " +
            "t_closed_timestamps_end." + DbOrgTimestamp.STRING + " AS " + CLOSED_TIME_END_STRING + ", " +
            "t_closed_timestamps_start." + DbOrgTimestamp.TIMESTAMP + " AS " + CLOSED_TIME_TIMESTAMP + ", " +
            ms2StartOfDay("t_closed_timestamps_start." + DbOrgTimestamp.TIMESTAMP) + " AS " + CLOSED_TIME_START_OF_DAY + ", " +
            "t_closed_timestamps_start." + DbOrgTimestamp.HOUR + " AS " + CLOSED_TIME_HOUR + ", " +

            "t_clock_range." + DbOrgRange.STRING + " AS " + CLOCK_RANGE_STRING + ", " +
            "t_clock_timestamps_start." + DbOrgTimestamp.STRING + " AS " + CLOCK_TIME_STRING + ", " +
            "t_clock_timestamps_end." + DbOrgTimestamp.STRING + " AS " + CLOCK_TIME_END_STRING + ", " +

            "t_books." + DbBook.NAME + " AS " + BOOK_NAME + " " +

            "FROM " + DbNote.TABLE + " " +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_scheduled_range", DbOrgRange._ID, DbNote.TABLE, DbNote.SCHEDULED_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_scheduled_timestamps_start", DbOrgTimestamp._ID, "t_scheduled_range", DbOrgRange.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_scheduled_timestamps_end", DbOrgTimestamp._ID, "t_scheduled_range", DbOrgRange.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_deadline_range", DbOrgRange._ID, DbNote.TABLE, DbNote.DEADLINE_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_deadline_timestamps_start", DbOrgTimestamp._ID, "t_deadline_range", DbOrgRange.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_deadline_timestamps_end", DbOrgTimestamp._ID, "t_deadline_range", DbOrgRange.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_closed_range", DbOrgRange._ID, DbNote.TABLE, DbNote.CLOSED_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_closed_timestamps_start", DbOrgTimestamp._ID, "t_closed_range", DbOrgRange.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_closed_timestamps_end", DbOrgTimestamp._ID, "t_closed_range", DbOrgRange.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "t_clock_range", DbOrgRange._ID, DbNote.TABLE, DbNote.CLOCK_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_clock_timestamps_start", DbOrgTimestamp._ID, "t_clock_range", DbOrgRange.START_TIMESTAMP_ID) +
            GenericDatabaseUtils.join(DbOrgTimestamp.TABLE, "t_clock_timestamps_end", DbOrgTimestamp._ID, "t_clock_range", DbOrgRange.END_TIMESTAMP_ID) +

            GenericDatabaseUtils.join(DbBook.TABLE, "t_books", DbBook._ID, DbNote.TABLE, DbNote.BOOK_ID) +

            GenericDatabaseUtils.join(DbNoteAncestor.TABLE, "t_note_ancestors", DbNoteAncestor.NOTE_ID, DbNote.TABLE, DbNote._ID) +
            GenericDatabaseUtils.join(DbNote.TABLE, "t_notes_with_inherited_tags", DbNote._ID, "t_note_ancestors", DbNoteAncestor.ANCESTOR_NOTE_ID) +

            " GROUP BY " + GenericDatabaseUtils.field(DbNote.TABLE, DbNote._ID);
}
