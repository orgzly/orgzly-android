package com.orgzly.android.provider.views;

import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbOrgRange;
import com.orgzly.android.provider.models.DbOrgTimestamp;

public class TimesView {
    public static final String VIEW_NAME = "times_view";

    public static final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    public static final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +
            "  SELECT\n" +
            "  n." + DbNote.Column._ID + " as " + Columns.NOTE_ID + ",\n" +
            "  n." + DbNote.Column.BOOK_ID + " as " + Columns.BOOK_ID + ",\n" +
            "  coalesce(b." + DbBook.Column.TITLE + ", b." + DbBook.Column.NAME + ") as " + Columns.BOOK_NAME + ",\n" +
            "  n." + DbNote.Column.STATE + " as " + Columns.NOTE_STATE + ",\n" +
            "  n." + DbNote.Column.TITLE + " as " + Columns.NOTE_TITLE + ",\n" +
            "  1 as " + Columns.TIME_TYPE + ",\n" +
            "  t." + DbOrgTimestamp.Column.STRING + " as " + Columns.ORG_TIMESTAMP_STRING + "\n" +
            "  FROM " + DbOrgRange.TABLE + " r\n" +
            "  JOIN " + DbOrgTimestamp.TABLE + " t ON (r." + DbOrgRange.Column.START_TIMESTAMP_ID + " = t." + DbOrgTimestamp.Column._ID + " )\n" +
            "  JOIN " + DbNote.TABLE + " n ON (r." + DbOrgRange.Column._ID + " = n." + DbNote.Column.SCHEDULED_RANGE_ID + ")\n" +
            "  JOIN " + DbBook.TABLE + " b ON (b." + DbBook.Column._ID + " = n." + DbNote.Column.BOOK_ID + ")\n" +
            "  WHERE t." + DbOrgTimestamp.Columns.IS_ACTIVE + " = 1\n" +
            "UNION\n" +
            "  SELECT\n" +
            "  n." + DbNote.Column._ID + " as " + Columns.NOTE_ID + ",\n" +
            "  n." + DbNote.Column.BOOK_ID + " as " + Columns.BOOK_ID + ",\n" +
            "  coalesce(b." + DbBook.Column.TITLE + ", b." + DbBook.Column.NAME + ") as " + Columns.BOOK_NAME + ",\n" +
            "  n." + DbNote.Column.STATE + " as " + Columns.NOTE_STATE + ",\n" +
            "  n." + DbNote.Column.TITLE + " as " + Columns.NOTE_TITLE + ",\n" +
            "  1 as " + Columns.TIME_TYPE + ",\n" +
            "  t." + DbOrgTimestamp.Column.STRING + " as " + Columns.ORG_TIMESTAMP_STRING + "\n" +
            "  FROM " + DbOrgRange.TABLE + " r\n" +
            "  JOIN " + DbOrgTimestamp.TABLE + " t ON (r." + DbOrgRange.Column.START_TIMESTAMP_ID + " = t." + DbOrgTimestamp.Column._ID + " )\n" +
            "  JOIN " + DbNote.TABLE + " n ON (r." + DbOrgRange.Column._ID + " = n." + DbNote.Column.DEADLINE_RANGE_ID + ")\n" +
            "  JOIN " + DbBook.TABLE + " b ON (b." + DbBook.Column._ID + " = n." + DbNote.Column.BOOK_ID + ")\n" +
            "  WHERE t." + DbOrgTimestamp.Columns.IS_ACTIVE + " = 1\n";

    public interface Columns {
        String NOTE_ID = "note_id";
        String BOOK_ID = "book_id";
        String BOOK_NAME = "book_name";
        String NOTE_STATE = "note_state";
        String NOTE_TITLE = "note_title";
        String TIME_TYPE = "time_type";
        String ORG_TIMESTAMP_STRING = "org_timestamp_string";
    }
}
