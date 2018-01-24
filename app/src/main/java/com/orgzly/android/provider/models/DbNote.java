package com.orgzly.android.provider.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.OrgHead;
import com.orgzly.org.OrgProperties;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

import java.util.List;

/**
 * Notes.
 */
public class DbNote implements DbNoteColumns, BaseColumns {
    public static final String TABLE = "notes";

    public static final String[] CREATE_SQL = {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            CREATED_AT + " INTEGER DEFAULT 0," +

            /* Position/structure */
            BOOK_ID + " INTEGER NOT NULL," +
            POSITION + " INTEGER NOT NULL," +
            LFT + " INTEGER," +
            RGT + " INTEGER," +
            LEVEL + " INTEGER NOT NULL," +
            PARENT_ID + " INTEGER," +
            DESCENDANTS_COUNT + " INTEGER," +
            IS_FOLDED + " INTEGER," +
            FOLDED_UNDER_ID + " INTEGER," +
            IS_CUT + " INTEGER NOT NULL DEFAULT 0," +

            /* Payload */
            TITLE + " TEXT NOT NULL DEFAULT ''," +
            TAGS + " TEXT," +
            STATE + " TEXT," +
            PRIORITY + " TEXT," +
            CONTENT + " TEXT," +
            CONTENT_LINE_COUNT + " INTEGER," +

            /* Times */
            SCHEDULED_RANGE_ID + " INTEGER," +
            DEADLINE_RANGE_ID + " INTEGER," +
            CLOSED_RANGE_ID + " INTEGER," +
            CLOCK_RANGE_ID + " INTEGER)",

            /* For search. */
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + TITLE + " ON " + TABLE + "(" + TITLE + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + TAGS + " ON " + TABLE + "(" + TAGS + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + CONTENT + " ON " + TABLE + "(" + CONTENT + ")",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + BOOK_ID + " ON " + TABLE + "(" + BOOK_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + IS_CUT + " ON " + TABLE + "(" + IS_CUT + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + LFT + " ON " + TABLE + "(" + LFT + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + RGT + " ON " + TABLE + "(" + RGT + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + IS_FOLDED + " ON " + TABLE + "(" + IS_FOLDED + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + FOLDED_UNDER_ID + " ON " + TABLE + "(" + FOLDED_UNDER_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + PARENT_ID + " ON " + TABLE + "(" + PARENT_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + DESCENDANTS_COUNT + " ON " + TABLE + "(" + DESCENDANTS_COUNT + ")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
    public static String[] POSITION_PROJECTION = {
            BOOK_ID,
            LEVEL,
            LFT,
            RGT,
            DESCENDANTS_COUNT,
            FOLDED_UNDER_ID,
            PARENT_ID,
            IS_FOLDED
    };

    public static void toContentValues(SQLiteDatabase db, ContentValues values, OrgHead head) {
        values.put(TITLE, head.getTitle());

        values.put(PRIORITY, head.getPriority());

        values.put(STATE, head.getState());

        if (head.hasTags()) {
            values.put(TAGS, dbSerializeTags(head.getTags()));
        }

        if (head.hasScheduled()) {
            values.put(SCHEDULED_RANGE_ID, getOrInsertOrgRange(db, head.getScheduled()));
        }

        if (head.hasClosed()) {
            values.put(CLOSED_RANGE_ID, getOrInsertOrgRange(db, head.getClosed()));
        }

        if (head.hasClock()) {
            values.put(CLOCK_RANGE_ID, getOrInsertOrgRange(db, head.getClock()));
        }

        if (head.hasDeadline()) {
            values.put(DEADLINE_RANGE_ID, getOrInsertOrgRange(db, head.getDeadline()));
        }

        if (head.hasContent()) {
            values.put(CONTENT, head.getContent());
            values.put(CONTENT_LINE_COUNT, MiscUtils.lineCount(head.getContent()));
        }
    }

    /**
     * Convert to string that will be stored to database.
     */
    public static String dbSerializeTags(List tags) {
        return TextUtils.join(" ", tags);
    }

    /**
     * Parse string stored in database.
     */
    public static String[] dbDeSerializeTags(String str) {
        return str.split(" ");
    }

    /**
     * Gets {@link OrgDateTime} from database or inserts a new record if it doesn't exist.
     * @return {@link OrgDateTime} database ID
     */
    private static long getOrInsertOrgRange(SQLiteDatabase db, OrgRange range) {
        long id = DatabaseUtils.getId(
                db,
                DbOrgRange.TABLE,
                DbOrgRange.STRING + "=?",
                new String[] { range.toString() });

        if (id == 0) {
            ContentValues values = new ContentValues();

            long startTimestampId = getOrInsertOrgTime(db, range.getStartTime());

            long endTimestampId = 0;
            if (range.getEndTime() != null) {
                endTimestampId = getOrInsertOrgTime(db, range.getEndTime());
            }

            DbOrgRange.toContentValues(values, range, startTimestampId, endTimestampId);

            id = db.insertOrThrow(DbOrgRange.TABLE, null, values);
        }

        return id;
    }

    private static long getOrInsertOrgTime(SQLiteDatabase db, OrgDateTime orgDateTime) {
        long id = DatabaseUtils.getId(
                db,
                DbOrgTimestamp.TABLE,
                DbOrgTimestamp.STRING + "= ?",
                new String[] { orgDateTime.toString() });

        if (id == 0) {
            ContentValues values = new ContentValues();
            DbOrgTimestamp.toContentValues(values, orgDateTime);

            id = db.insertOrThrow(DbOrgTimestamp.TABLE, null, values);
        }

        return id;
    }

    public static void toContentValues(ContentValues values, NotePosition position) {
        values.put(BOOK_ID, position.getBookId());
        values.put(LEVEL, position.getLevel());
        values.put(LFT, position.getLft());
        values.put(RGT, position.getRgt());
        values.put(DESCENDANTS_COUNT, position.getDescendantsCount());
        values.put(FOLDED_UNDER_ID, position.getFoldedUnderId());
        values.put(PARENT_ID, position.getParentId());
        values.put(IS_FOLDED, position.isFolded() ? 1 : 0);
        values.put(POSITION, 0); // TODO: Remove
    }


    /**
     * Set created-at value from property.
     */
    public static void toContentValues(ContentValues values, OrgProperties properties, String createdAtProperty) {
        if (properties.containsKey(createdAtProperty)) { // Property found
            String value = properties.get(createdAtProperty);
            setCreatedAtValue(values, value);
        }
    }

    private static void setCreatedAtValue(ContentValues values, String value) {
        OrgRange range = OrgRange.doParse(value);
        if (range != null) {
            values.put(DbNote.CREATED_AT, range.getStartTime().getCalendar().getTimeInMillis());
        }
    }


    public static NotePosition positionFromCursor(Cursor cursor) {
        long bookId = cursor.getLong(cursor.getColumnIndex(BOOK_ID));
        int level = cursor.getInt(cursor.getColumnIndex(LEVEL));
        long lft = cursor.getLong(cursor.getColumnIndex(LFT));
        long rgt = cursor.getLong(cursor.getColumnIndex(RGT));
        int descendantsCount = cursor.getInt(cursor.getColumnIndex(DESCENDANTS_COUNT));
        long foldedUnderId = cursor.getLong(cursor.getColumnIndex(FOLDED_UNDER_ID));
        long parentId = cursor.getLong(cursor.getColumnIndex(PARENT_ID));
        int isFolded = cursor.getInt(cursor.getColumnIndex(IS_FOLDED));

        NotePosition position = new NotePosition();

        position.setLevel(level);
        position.setBookId(bookId);
        position.setLft(lft);
        position.setRgt(rgt);
        position.setDescendantsCount(descendantsCount);
        position.setFoldedUnderId(foldedUnderId);
        position.setParentId(parentId);
        position.setIsFolded(isFolded != 0);

        return position;
    }

    public static NotePosition getPosition(SQLiteDatabase db, long noteId) {
        Cursor cursor = db.query(
                TABLE,
                POSITION_PROJECTION,
                _ID + " = " + noteId,
                null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return positionFromCursor(cursor);

            } else {
                throw new IllegalStateException("Failed getting note for id " + noteId);
            }

        } finally {
            cursor.close();
        }
    }
}
