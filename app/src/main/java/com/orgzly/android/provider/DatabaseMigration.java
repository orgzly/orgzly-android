package com.orgzly.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.orgzly.BuildConfig;
import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.provider.models.DbNoteProperty;
import com.orgzly.android.provider.models.DbOrgTimestamp;
import com.orgzly.android.provider.models.DbProperty;
import com.orgzly.android.provider.models.DbPropertyName;
import com.orgzly.android.provider.models.DbPropertyValue;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.parser.OrgNestedSetParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database migration.
 *
 * All database schema updates and ugly fixes in one place.
 *
 * All names are hardcoded on purpose in case they are changed, so that values of constants
 * such as TITLE or LFT can simply be updated. It makes DatabaseSchema clean and always current.
 *
 * Eventually we can start removing these updates.  If user didn't update the app for a long
 * time, it should be safe to assume he doesn't need the data and can just re-install the app.
 * So this mess can be removed over time.
 */
public class DatabaseMigration {
    private static final String TAG = DatabaseMigration.class.getName();

    private static final int DB_VER_1 = 130;
    private static final int DB_VER_2 = 131;
    private static final int DB_VER_3 = 132;
    private static final int DB_VER_4 = 133;
    private static final int DB_VER_5 = 134;
    private static final int DB_VER_6 = 135;
    private static final int DB_VER_7 = 136;
    private static final int DB_VER_8 = 137;
    private static final int DB_VER_9 = 138;
    private static final int DB_VER_10 = 139;

    static final int DB_VER_CURRENT = DB_VER_10;

    /**
     * Start from the old version and go through all changes. No breaks.
     */
    public static void upgrade(SQLiteDatabase db, int oldVersion, Runnable notifyUserIfSlow) {

        /* Simulate slow upgrade. */
        // notifyUserIfSlow.run();
        // try { Thread.sleep(5000); } catch (InterruptedException e) { e.printStackTrace(); }

        switch (oldVersion) {
            case DB_VER_1:
                db.execSQL("ALTER TABLE books ADD COLUMN title"); // TITLE

            case DB_VER_2:
                db.execSQL("ALTER TABLE books ADD COLUMN is_indented INTEGER DEFAULT 0"); // IS_INDENTED
                db.execSQL("ALTER TABLE books ADD COLUMN used_encoding TEXT"); // USED_ENCODING
                db.execSQL("ALTER TABLE books ADD COLUMN detected_encoding TEXT"); // DETECTED_ENCODING
                db.execSQL("ALTER TABLE books ADD COLUMN selected_encoding TEXT"); // SELECTED_ENCODING

            case DB_VER_3:
                /* Views-only updates */

            case DB_VER_4:
                /* Folding notes implemented. */

                if (notifyUserIfSlow != null) {
                    notifyUserIfSlow.run();
                    notifyUserIfSlow = null;
                }

                db.execSQL("ALTER TABLE notes ADD COLUMN parent_id"); // PARENT_ID
                db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_is_visible ON notes(is_visible)"); // LFT
                db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_parent_position ON notes(parent_position)"); // RGT
                db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_is_collapsed ON notes(is_collapsed)"); // IS_FOLDED
                db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_is_under_collapsed ON notes(is_under_collapsed)"); // FOLDED_UNDER_ID
                db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_parent_id ON notes(parent_id)"); // PARENT_ID
                db.execSQL("CREATE INDEX IF NOT EXISTS i_notes_has_children ON notes(has_children)"); // DESCENDANTS_COUNT

                convertNotebooksFromPositionToNestedSet(db);

            case DB_VER_5:
                fixOrgRanges(db);

            case DB_VER_6:
                /* Properties moved from content. */

                if (notifyUserIfSlow != null) {
                    notifyUserIfSlow.run();
                    notifyUserIfSlow = null;
                }

                for (String sql : DbNoteProperty.CREATE_SQL) db.execSQL(sql);
                for (String sql : DbPropertyName.CREATE_SQL) db.execSQL(sql);
                for (String sql : DbPropertyValue.CREATE_SQL) db.execSQL(sql);
                for (String sql : DbProperty.CREATE_SQL) db.execSQL(sql);

                movePropertiesFromBody(db);

            case DB_VER_7:
                encodeRookUris(db);

            case DB_VER_8:
                for (String sql : DbNoteAncestor.CREATE_SQL) db.execSQL(sql);
                populateNoteAncestors(db);

            case DB_VER_9:
                migrateOrgTimestamps(db);
        }
    }

    private static void migrateOrgTimestamps(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE org_timestamps RENAME TO org_timestamps_prev");

        for (String sql : DbOrgTimestamp.CREATE_SQL) db.execSQL(sql);

        Cursor cursor = db.query(
                "org_timestamps_prev", new String[] { "_id", "string" }, null, null, null, null, null);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String string = cursor.getString(1);

                OrgDateTime orgDateTime = OrgDateTime.parse(string);

                ContentValues values = new ContentValues();
                values.put("_id", id);
                DbOrgTimestamp.toContentValues(values, orgDateTime);

                db.insert("org_timestamps", null, values);
            }
        } finally {
            cursor.close();
        }

        db.execSQL("DROP TABLE org_timestamps_prev");
    }

    private static void populateNoteAncestors(SQLiteDatabase db) {
        db.execSQL("INSERT INTO note_ancestors (book_id, note_id, ancestor_note_id) " +
                   "SELECT n.book_id, n._id, a._id FROM notes n " +
                   "JOIN notes a on (n.book_id = a.book_id AND a.is_visible < n.is_visible AND n.parent_position < a.parent_position) " +
                   "WHERE a.level > 0");
    }

    private static void movePropertiesFromBody(SQLiteDatabase db) {
        Cursor cursor = db.query("notes", new String[] { "_id", "content" }, null, null, null, null, null);

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long noteId = cursor.getLong(0);
                String content = cursor.getString(1);

                if (!TextUtils.isEmpty(content)) {
                    StringBuilder newContent = new StringBuilder();
                    List<String[]> properties = getPropertiesFromContent(content, newContent);

                    if (properties.size() > 0) {
                        int pos = 1;
                        for (String[] property: properties) {
                            long nameId = DbPropertyName.getOrInsert(db, property[0]);
                            long valueId = DbPropertyValue.getOrInsert(db, property[1]);
                            long propertyId = DbProperty.getOrInsert(db, nameId, valueId);
                            DbNoteProperty.getOrInsert(db, noteId, pos++, propertyId);
                        }

                        /* Update content and its line count */
                        ContentValues values = new ContentValues();
                        values.put("content", newContent.toString());
                        values.put("content_line_count", MiscUtils.lineCount(newContent.toString()));
                        db.update("notes", values, "_id = " + noteId, null);
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    public static List<String[]> getPropertiesFromContent(String content, StringBuilder newContent) {
        List<String[]> properties = new ArrayList<>();

        final Pattern propertiesPattern = Pattern.compile("^\\s*:PROPERTIES:(.*?):END: *\n*(.*)", Pattern.DOTALL);
        final Pattern propertyPattern = Pattern.compile("^:([^:\\s]+):\\s+(.*)\\s*$");

        Matcher m = propertiesPattern.matcher(content);

        if (m.find()) {
            for (String propertyLine: m.group(1).split("\n")) {
                Matcher pm = propertyPattern.matcher(propertyLine.trim());

                if (pm.find()) {
                    // Add name-value pair
                    properties.add(new String[] { pm.group(1), pm.group(2) });
                }
            }

            newContent.append(m.group(2));
        }

        return properties;
    }

    private static void convertNotebooksFromPositionToNestedSet(SQLiteDatabase db) {
        Cursor cursor = db.query("books", new String[] { "_id" }, null, null, null, null, null);

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long bookId = cursor.getLong(0);
                convertNotebookFromPositionToNestedSet(db, bookId);
                updateParentIds(db, bookId);
            }
        } finally {
            cursor.close();
        }
    }

    private static void convertNotebookFromPositionToNestedSet(SQLiteDatabase db, long bookId) {
        /* Insert root note. */
        ContentValues rootNoteValues = new ContentValues();
        rootNoteValues.put("level", 0);
        rootNoteValues.put("book_id", bookId);
        rootNoteValues.put("position", 0); // TODO: Remove
        db.insertOrThrow("notes", null, rootNoteValues);

        Cursor cursor = db.query(
                "notes",
                null,
                "book_id = " + bookId,
                null,
                null,
                null,
                "position");

        try {
            updateNotesPositionsFromLevel(db, cursor);

        } finally {
            cursor.close();
        }
    }

    private static void updateParentIds(SQLiteDatabase db, long bookId) {
        String parentId = "(SELECT _id FROM notes AS n WHERE " +
                          "book_id = " + bookId + " AND " +
                          "n.is_visible < notes.is_visible AND " +
                          "notes.parent_position < n.parent_position ORDER BY n.is_visible DESC LIMIT 1)";

        db.execSQL("UPDATE notes SET parent_id = " + parentId +
                   " WHERE book_id = " + bookId + " AND is_cut = 0 AND level > 0");
    }

    private static void updateNotesPositionsFromLevel(SQLiteDatabase db, Cursor cursor) {
        Stack<NotePositionWithId> stack = new Stack<>();
        int prevLevel = -1;
        long sequence = OrgNestedSetParser.STARTING_VALUE - 1;


        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            NotePositionWithId note = new NotePositionWithId();
            note.id = cursor.getLong(cursor.getColumnIndex(DbNote.Column._ID));
            note.position = DbNote.positionFromCursor(cursor);

            if (prevLevel < note.position.getLevel()) {
                /*
                 * This is a descendant of previous thisNode.
                 */

                /* Put the current thisNode on the stack. */
                sequence += 1;
                note.position.setLft(sequence);
                stack.push(note);

            } else if (prevLevel == note.position.getLevel()) {
                /*
                 * This is a sibling, which means that the last thisNode visited can be completed.
                 * Take it off the stack, update its rgt value and announce it.
                 */
                NotePositionWithId nodeFromStack = stack.pop();
                sequence += 1;
                nodeFromStack.position.setRgt(sequence);
                calculateAndSetDescendantsCount(nodeFromStack.position, 1);
                updateNotePositionValues(db, nodeFromStack);

                /* Put the current thisNode on the stack. */
                sequence += 1;
                note.position.setLft(sequence);
                stack.push(note);

            } else {
                /*
                 * Note has lower level then the previous one - we're out of the set.
                 * Start popping the stack, up to and including the thisNode with the same level.
                 */
                while (!stack.empty()) {
                    NotePositionWithId nodeFromStack = stack.peek();

                    if (nodeFromStack.position.getLevel() >= note.position.getLevel()) {
                        stack.pop();

                        sequence += 1;
                        nodeFromStack.position.setRgt(sequence);
                        calculateAndSetDescendantsCount(nodeFromStack.position, 1);
                        updateNotePositionValues(db, nodeFromStack);

                    } else {
                        break;
                    }
                }

                    /* Put the current thisNode on the stack. */
                sequence += 1;
                note.position.setLft(sequence);
                stack.push(note);
            }

            prevLevel = note.position.getLevel();
        }

        /* Pop remaining nodes. */
        while (! stack.empty()) {
            NotePositionWithId nodeFromStack = stack.pop();
            sequence += 1;
            nodeFromStack.position.setRgt(sequence);
            calculateAndSetDescendantsCount(nodeFromStack.position, 1);
            updateNotePositionValues(db, nodeFromStack);
        }
    }


    private static class NotePositionWithId {
        long id;
        NotePosition position;
    }

    private static int updateNotePositionValues(SQLiteDatabase db, NotePositionWithId note) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updating " + note.id + " with: " + note.position);

        ContentValues values = new ContentValues();
        DbNote.toContentValues(values, note.position);

        return db.update(DbNote.TABLE, values, DbNote.Column._ID + " = " + note.id, null);
    }

    private static void calculateAndSetDescendantsCount(NotePosition node, int gap) {
        int n = (int) (node.getRgt() - node.getLft() - gap) / ( 2 * gap );

        node.setDescendantsCount(n);
    }

    /**
     * 1.4-beta.1 misused insertWithOnConflict due to
     * https://code.google.com/p/android/issues/detail?id=13045.
     *
     * org_ranges ended up with duplicates having -1 for
     * start_timestamp_id and end_timestamp_id.
     *
     * Delete those, update notes tables and add a unique constraint to the org_ranges.
     */
    private static void fixOrgRanges(SQLiteDatabase db) {
        String[] notesFields = new String[] {
                "scheduled_range_id",
                "deadline_range_id",
                "closed_range_id",
                "clock_range_id"
        };

        Cursor cursor = db.query(
                "org_ranges",
                new String[] { "_id", "string" },
                "start_timestamp_id = -1 OR end_timestamp_id = -1",
                null, null, null, null);

        try {
            /* Go through all invalid entries. */
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String string = cursor.getString(1);

                String validTimestampId = "(start_timestamp_id IS NULL OR start_timestamp_id != -1) AND (end_timestamp_id IS NULL OR end_timestamp_id != -1)";
                String validEntry = "(SELECT _id FROM org_ranges WHERE string = " + android.database.DatabaseUtils.sqlEscapeString(string) + " and " + validTimestampId + ")";

                for (String field: notesFields) {
                    db.execSQL("UPDATE notes SET " + field + " = " + validEntry + " WHERE " + field + " = " + id);
                }
            }
        } finally {
            cursor.close();
        }

        db.execSQL("DELETE FROM org_ranges WHERE start_timestamp_id = -1 OR end_timestamp_id = -1");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS i_org_ranges_string ON org_ranges(string)");
    }

    /**
     * file:/dir/file name.org
     * file:/dir/file%20name.org
     */
    private static void encodeRookUris(SQLiteDatabase db) {
        Cursor cursor = db.query("rook_urls", new String[] { "_id", "rook_url" }, null, null, null, null, null);

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String uri = cursor.getString(1);

                String encodedUri = MiscUtils.encodeUri(uri);

                if (! uri.equals(encodedUri)) {
                    /* Update unless same URL already exists. */
                    Cursor c = db.query("rook_urls", new String[] { "_id" },
                            "rook_url = ?", new String[] { encodedUri }, null, null, null);
                    try {
                        if (!c.moveToFirst()) {
                            ContentValues values = new ContentValues();
                            values.put("rook_url", encodedUri);
                            db.update("rook_urls", values, "_id = " + id, null);
                        }
                    } finally {
                        c.close();
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }
}
