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
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

import java.util.List;

/**
 * Notes.
 */
public class DbNote {
    public static final String TABLE = "notes";

    public static final String[] CREATE_SQL = new String[] {
            "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

            /* Position/structure */
            Columns.BOOK_ID + " INTEGER NOT NULL," +
            Columns.POSITION + " INTEGER NOT NULL," +
            Columns.LFT + " INTEGER," +
            Columns.RGT + " INTEGER," +
            Columns.LEVEL + " INTEGER NOT NULL," +
            Columns.PARENT_ID + " INTEGER," +
            Columns.DESCENDANTS_COUNT + " INTEGER," +
            Columns.IS_FOLDED + " INTEGER," +
            Columns.FOLDED_UNDER_ID + " INTEGER," +
            Columns.IS_CUT + " INTEGER NOT NULL DEFAULT 0," +

            /* Payload */
            Columns.TITLE + " TEXT NOT NULL DEFAULT ''," +
            Columns.TAGS + " TEXT," +
            Columns.STATE + " TEXT," +
            Columns.PRIORITY + " TEXT," +
            Columns.CONTENT + " TEXT," +
            Columns.CONTENT_LINE_COUNT + " INTEGER," +

            /* Times */
            Columns.SCHEDULED_RANGE_ID + " INTEGER," +
            Columns.DEADLINE_RANGE_ID + " INTEGER," +
            Columns.CREATED_AT + " INTEGER," +
            Columns.CREATED_AT_INTERNAL + " INTEGER," +
            Columns.CLOSED_RANGE_ID + " INTEGER," +
            Columns.CLOCK_RANGE_ID + " INTEGER)",

            /* For search. */
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.TITLE + " ON " + TABLE + "(" + Columns.TITLE + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.TAGS + " ON " + TABLE + "(" + Columns.TAGS + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.CONTENT + " ON " + TABLE + "(" + Columns.CONTENT + ")",

            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.BOOK_ID + " ON " + TABLE + "(" + Columns.BOOK_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.IS_CUT + " ON " + TABLE + "(" + Columns.IS_CUT + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.LFT + " ON " + TABLE + "(" + Columns.LFT + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.RGT + " ON " + TABLE + "(" + Columns.RGT + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.IS_FOLDED + " ON " + TABLE + "(" + Columns.IS_FOLDED + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.FOLDED_UNDER_ID + " ON " + TABLE + "(" + Columns.FOLDED_UNDER_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.PARENT_ID + " ON " + TABLE + "(" + Columns.PARENT_ID + ")",
            "CREATE INDEX IF NOT EXISTS i_" + TABLE + "_" + Columns.DESCENDANTS_COUNT + " ON " + TABLE + "(" + Columns.DESCENDANTS_COUNT + ")"
    };

    public static final String DROP_SQL = "DROP TABLE IF EXISTS " + TABLE;
    public static String[] POSITION_PROJECTION = new String[] {
            Column.BOOK_ID,
            Column.LEVEL,
            Column.LFT,
            Column.RGT,
            Column.DESCENDANTS_COUNT,
            Column.FOLDED_UNDER_ID,
            Column.PARENT_ID,
            Column.IS_FOLDED
    };

    public static void toContentValues(SQLiteDatabase db, ContentValues values, OrgHead head, String createdProp) {
        values.put(Column.TITLE, head.getTitle());

        values.put(Column.PRIORITY, head.getPriority());

        values.put(Column.STATE, head.getState());

        if (head.hasTags()) {
            values.put(Column.TAGS, dbSerializeTags(head.getTags()));
        }

        if (head.hasScheduled()) {
            values.put(Column.SCHEDULED_RANGE_ID, getOrInsertOrgRange(db, head.getScheduled()));
        }

        if (head.hasClosed()) {
            values.put(Column.CLOSED_RANGE_ID, getOrInsertOrgRange(db, head.getClosed()));
        }

        if (head.hasClock()) {
            values.put(Column.CLOCK_RANGE_ID, getOrInsertOrgRange(db, head.getClock()));
        }

        if (head.hasDeadline()) {
            values.put(Column.DEADLINE_RANGE_ID, getOrInsertOrgRange(db, head.getDeadline()));
        }

        for (int i = 0; i < head.getProperties().size(); i++) {
            if (head.getProperties().get(i).getName().equals(createdProp)) {
                try {
                    OrgDateTime x = OrgDateTime.parse(head.getProperties().get(i).getValue());
                    values.put(Column.CREATED_AT, x.getCalendar().getTimeInMillis());
                    break;
                } catch (IllegalArgumentException e) {
                    // Parsing failed, give up immediately
                    break;
                }
            }
        }

        if (head.hasContent()) {
            values.put(Column.CONTENT, head.getContent());
            values.put(Column.CONTENT_LINE_COUNT, MiscUtils.lineCount(head.getContent()));
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
                DbOrgRange.Column.STRING + "=?",
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
                DbOrgTimestamp.Column.STRING + "= ?",
                new String[] { orgDateTime.toString() });

        if (id == 0) {
            ContentValues values = new ContentValues();
            DbOrgTimestamp.toContentValues(values, orgDateTime);

            id = db.insertOrThrow(DbOrgTimestamp.TABLE, null, values);
        }

        return id;
    }

    public static void toContentValues(ContentValues values, NotePosition position) {
        values.put(Column.BOOK_ID, position.getBookId());
        values.put(Column.LEVEL, position.getLevel());
        values.put(Column.LFT, position.getLft());
        values.put(Column.RGT, position.getRgt());
        values.put(Column.DESCENDANTS_COUNT, position.getDescendantsCount());
        values.put(Column.FOLDED_UNDER_ID, position.getFoldedUnderId());
        values.put(Column.PARENT_ID, position.getParentId());
        values.put(Column.IS_FOLDED, position.isFolded() ? 1 : 0);
        values.put(Column.POSITION, 0); // TODO: Remove
    }

    public static NotePosition positionFromCursor(Cursor cursor) {
        long bookId = cursor.getLong(cursor.getColumnIndex(Column.BOOK_ID));
        int level = cursor.getInt(cursor.getColumnIndex(Column.LEVEL));
        long lft = cursor.getLong(cursor.getColumnIndex(Column.LFT));
        long rgt = cursor.getLong(cursor.getColumnIndex(Column.RGT));
        int descendantsCount = cursor.getInt(cursor.getColumnIndex(Column.DESCENDANTS_COUNT));
        long foldedUnderId = cursor.getLong(cursor.getColumnIndex(Column.FOLDED_UNDER_ID));
        long parentId = cursor.getLong(cursor.getColumnIndex(Column.PARENT_ID));
        int isFolded = cursor.getInt(cursor.getColumnIndex(Column.IS_FOLDED));

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
                Column._ID + " = " + noteId,
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

    public interface Columns {
        String BOOK_ID = "book_id";
        String POSITION = "position";

        String LEVEL = "level";
        String TITLE = "title";
        String TAGS = "tags";
        String STATE = "state";
        String PRIORITY = "priority";

        String SCHEDULED_RANGE_ID = "scheduled_range_id";
        String DEADLINE_RANGE_ID = "deadline_range_id";
        String CREATED_AT = "created_at";
        String CREATED_AT_INTERNAL = "created_at_internal";
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

    public static class Column implements Columns, BaseColumns {}
}
