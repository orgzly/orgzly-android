package com.orgzly.android.provider.views;

import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbOrgRange;
import com.orgzly.android.provider.models.DbOrgTimestamp;

public class DbTimeView implements DbTimeViewColumns {
    public static final String VIEW_NAME = "times_view";

    public static final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    public static final int SCHEDULED_TIME = 1;
    public static final int DEADLINE_TIME = 2;

    public static final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +
            "  SELECT\n" +
            "  n." + DbNote._ID + " as " + NOTE_ID + ",\n" +
            "  n." + DbNote.BOOK_ID + " as " + BOOK_ID + ",\n" +
            "  coalesce(b." + DbBook.TITLE + ", b." + DbBook.NAME + ") as " + BOOK_NAME + ",\n" +
            "  n." + DbNote.STATE + " as " + NOTE_STATE + ",\n" +
            "  n." + DbNote.TITLE + " as " + NOTE_TITLE + ",\n" +
            "  " + + SCHEDULED_TIME + " as " + TIME_TYPE + ",\n" +
            "  t." + DbOrgTimestamp.STRING + " as " + ORG_TIMESTAMP_STRING + "\n" +
            "  FROM " + DbOrgRange.TABLE + " r\n" +
            "  JOIN " + DbOrgTimestamp.TABLE + " t ON (r." + DbOrgRange.START_TIMESTAMP_ID + " = t." + DbOrgTimestamp._ID + " )\n" +
            "  JOIN " + DbNote.TABLE + " n ON (r." + DbOrgRange._ID + " = n." + DbNote.SCHEDULED_RANGE_ID + ")\n" +
            "  JOIN " + DbBook.TABLE + " b ON (b." + DbBook._ID + " = n." + DbNote.BOOK_ID + ")\n" +
            "  WHERE t." + DbOrgTimestamp.IS_ACTIVE + " = 1\n" +
            "UNION\n" +
            "  SELECT\n" +
            "  n." + DbNote._ID + " as " + NOTE_ID + ",\n" +
            "  n." + DbNote.BOOK_ID + " as " + BOOK_ID + ",\n" +
            "  coalesce(b." + DbBook.TITLE + ", b." + DbBook.NAME + ") as " + BOOK_NAME + ",\n" +
            "  n." + DbNote.STATE + " as " + NOTE_STATE + ",\n" +
            "  n." + DbNote.TITLE + " as " + NOTE_TITLE + ",\n" +
            "  " + DEADLINE_TIME + " as " + TIME_TYPE + ",\n" +
            "  t." + DbOrgTimestamp.STRING + " as " + ORG_TIMESTAMP_STRING + "\n" +
            "  FROM " + DbOrgRange.TABLE + " r\n" +
            "  JOIN " + DbOrgTimestamp.TABLE + " t ON (r." + DbOrgRange.START_TIMESTAMP_ID + " = t." + DbOrgTimestamp._ID + " )\n" +
            "  JOIN " + DbNote.TABLE + " n ON (r." + DbOrgRange._ID + " = n." + DbNote.DEADLINE_RANGE_ID + ")\n" +
            "  JOIN " + DbBook.TABLE + " b ON (b." + DbBook._ID + " = n." + DbNote.BOOK_ID + ")\n" +
            "  WHERE t." + DbOrgTimestamp.IS_ACTIVE + " = 1\n";

}
