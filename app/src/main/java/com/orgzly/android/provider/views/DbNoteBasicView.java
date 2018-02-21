package com.orgzly.android.provider.views;

import android.provider.BaseColumns;

import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteColumns;
import com.orgzly.android.provider.models.DbOrgRange;

/**
 * Notes with planning times.
 */
public class DbNoteBasicView implements DbNoteBasicViewColumns, DbNoteColumns, BaseColumns  {
    public static final String VIEW_NAME = "notes_basic_view";

    public static final String DROP_SQL = "DROP VIEW IF EXISTS " + VIEW_NAME;

    public static final String CREATE_SQL =
            "CREATE VIEW " + VIEW_NAME + " AS " +

            "SELECT " + DbNote.TABLE + ".*, " +

            "a." + DbOrgRange.STRING + " AS " + SCHEDULED_RANGE_STRING + ", " +
            "b." + DbOrgRange.STRING + " AS " + DEADLINE_RANGE_STRING + ", " +
            "c." + DbOrgRange.STRING + " AS " + CLOSED_RANGE_STRING + ", " +
            "d." + DbOrgRange.STRING + " AS " + CLOCK_RANGE_STRING + " " +

            "FROM " + DbNote.TABLE + " " +

            GenericDatabaseUtils.join(DbOrgRange.TABLE, "a", DbOrgRange._ID, DbNote.TABLE, DbNote.SCHEDULED_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgRange.TABLE, "b", DbOrgRange._ID, DbNote.TABLE, DbNote.DEADLINE_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgRange.TABLE, "c", DbOrgRange._ID, DbNote.TABLE, DbNote.CLOSED_RANGE_ID) +
            GenericDatabaseUtils.join(DbOrgRange.TABLE, "d", DbOrgRange._ID, DbNote.TABLE, DbNote.CLOCK_RANGE_ID)
            ;
}
