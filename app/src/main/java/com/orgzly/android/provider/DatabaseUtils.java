package com.orgzly.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.BuildConfig;
import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.LogUtils;

/**
 * Orgzly-specific {@link GenericDatabaseUtils}.
 */
public class DatabaseUtils {
    private static final String TAG = DatabaseUtils.class.getName();

    public static final String[] PROJECTION_FOR_ID = { "_id" };
    public static final String[] PROJECTION_FOR_COUNT = { "count(*)" };

    public static final String WHERE_EXISTING_NOTES = "(" +
                                                      DbNote.IS_CUT + " = 0" + " AND " +
                                                      DbNote.LEVEL + " > 0" + ")"; // Root note is a dummy note with level 0

    public static final String WHERE_VISIBLE_NOTES = "(" +
                                                     WHERE_EXISTING_NOTES + " AND " +
                                                     GenericDatabaseUtils.whereNullOrZero(DbNote.FOLDED_UNDER_ID) + ")";

    public static final String WHERE_NOTES_WITH_TIMES = "(" + DbNote.SCHEDULED_RANGE_ID + " IS NOT NULL OR " + DbNote.DEADLINE_RANGE_ID + " IS NOT NULL)";

    public static String whereUncutBookNotes(long bookId) {
        return "(" + DbNote.BOOK_ID + " = " + bookId + " AND " + WHERE_EXISTING_NOTES + ")";
    }

    public static String whereAncestors(long bookId, long lft, long rgt) {
        return "(" + whereUncutBookNotes(bookId) + " AND " +
               DbNote.LFT + "< " + lft + " AND " + rgt + " < " + DbNote.RGT + ")";
    }

    public static String whereAncestorsAndNote(long bookId, long id) {
        return "(" + whereAncestors(bookId, String.valueOf(id)) + " OR (" + DbNote._ID + " = " + id + "))";
    }

    public static String whereAncestors(long bookId, String ids) {
        String sql = "(" + DbNote._ID + " IN (SELECT DISTINCT b." + DbNote._ID + " FROM " +
                     DbNote.TABLE + " a, " + DbNote.TABLE + " b WHERE " +
                     "a." + DbNote.BOOK_ID + " = " + bookId + " AND " +
                     "b." + DbNote.BOOK_ID + " = " + bookId + " AND " +
                     "a." + DbNote._ID + " IN (" + ids + ") AND " +
                     "b." + DbNote.LFT + " < a." + DbNote.LFT + " AND a." + DbNote.RGT + " < b." + DbNote.RGT + "))";

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "whereAncestors: " + sql);

        return sql;
    }

    public static String ancestorsIds(SQLiteDatabase db, long bookId, long noteId) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] { "group_concat(" + DbNote._ID + ", ',')" },
                DatabaseUtils.whereAncestors(bookId, String.valueOf(noteId)),
                null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    public static String whereDescendants(long bookId, long lft, long rgt) {
        return "(" + whereUncutBookNotes(bookId) + " AND " +
               lft + " < " + DbNote.LFT + " AND " + DbNote.RGT + " < " + rgt + ")";
    }

    public static String whereDescendantsAndNote(long bookId, long lft, long rgt) {
        return "(" + whereUncutBookNotes(bookId) + " AND " +
               lft + " <= " + DbNote.LFT + " AND " + DbNote.RGT + " <= " + rgt + ")";
    }

    public static String whereDescendantsAndNotes(long bookId, String ids) {
        return DbNote._ID + " IN (SELECT DISTINCT d." + DbNote._ID + " FROM " +
               DbNote.TABLE + " n, " + DbNote.TABLE + " d WHERE " +
               "d." + DbNote.BOOK_ID + " = " + bookId + " AND " +
               "n." + DbNote.BOOK_ID + " = " + bookId + " AND " +
               "n." + DbNote._ID + " IN (" + ids + ") AND " +
               "d." + DbNote.IS_CUT + " = 0 AND " +
               "n." + DbNote.LFT + " <= d." + DbNote.LFT + " AND d." + DbNote.RGT + " <= n." + DbNote.RGT + ")";
    }

    public static long getId(SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
        Cursor cursor = db.query(table, DatabaseUtils.PROJECTION_FOR_ID, selection, selectionArgs, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else {
                return 0;
            }

        } finally {
            cursor.close();
        }
    }

    /**
     * Sets modification time for notebook to now.
     */
    public static int updateBookMtime(SQLiteDatabase db, long bookId) {
        return DatabaseUtils.updateBookMtime(db, DbBook._ID + "=" + bookId, null);
    }

    /**
     * Sets modification time for selected notebooks to now.
     */
    public static int updateBookMtime(SQLiteDatabase db, String where, String[] whereArgs) {
        ContentValues values = new ContentValues();

        values.put(DbBook.MTIME, System.currentTimeMillis());

        return db.update(DbBook.TABLE, values, where, whereArgs);
    }

    /**
     * Increments note's lft and rgt to make space for new notes.
     *
     * @return available lft and rgt which can be occupied by new notes.
     */
    public static long[] makeSpaceForNewNotes(SQLiteDatabase db, int numberOfNotes, NotePosition targetNotePosition, Place place) { // TODO: Book ID not checked
        long lft;
        int level;

        int spaceRequired = numberOfNotes * 2;

        String selection;

        String bookSelection = whereUncutBookNotes(targetNotePosition.getBookId());

        switch (place) {

            case ABOVE:
                selection = bookSelection + " AND " + DbNote.LFT + " >= " + targetNotePosition.getLft();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.LFT);

                selection = bookSelection + " AND " + DbNote.RGT + " > " + targetNotePosition.getLft();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.RGT);

                lft = targetNotePosition.getLft();
                level = targetNotePosition.getLevel();

                break;

            case UNDER:
                selection = bookSelection + " AND " + DbNote.LFT + " > " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.LFT);

                selection = bookSelection + " AND " + DbNote.RGT + " >= " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.RGT);

                lft = targetNotePosition.getRgt();
                level = targetNotePosition.getLevel() + 1;

                break;

            case BELOW:
                selection = bookSelection + " AND " + DbNote.LFT + " > " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.LFT);

                // Container notes - update their RGT.
                selection = bookSelection + " AND " + DbNote.RGT + " > " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.RGT);

                lft = targetNotePosition.getRgt() + 1;
                level = targetNotePosition.getLevel();

                break;

            default:
                throw new IllegalArgumentException("Unsupported paste relative position " + place);
        }

        return new long[] { lft, level };
    }

    public static void updateDescendantsCount(SQLiteDatabase db, String where) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] { DbNote._ID, DbNote.LFT, DbNote.RGT, DbNote.BOOK_ID },
                where,
                null, null, null, null);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);
                long lft = cursor.getLong(1);
                long rgt = cursor.getLong(2);
                long bookId = cursor.getLong(3);

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Updating descendants for note #" + id + " (" + lft + "-" + rgt + ")");

                String descendantsCount = "(SELECT count(*) FROM " + DbNote.TABLE +
                                          " WHERE " + whereDescendants(bookId, lft, rgt) + ")";

                String sql = "UPDATE " + DbNote.TABLE +
                             " SET " + DbNote.DESCENDANTS_COUNT + " = " + descendantsCount +
                             " WHERE " + DbNote._ID + " = " + id;

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sql);

                db.execSQL(sql);
            }

        } finally {
            cursor.close();
        }
    }

    public static long getPreviousSiblingId(SQLiteDatabase db, NotePosition n) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                PROJECTION_FOR_ID,
                DatabaseUtils.whereUncutBookNotes(n.getBookId()) + " AND " +
                DbNote.LFT + " < " + n.getLft() + " AND " +
                DbNote.PARENT_ID + " = " + n.getParentId(),
                null,
                null,
                null,
                DbNote.LFT + " DESC");
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }

        return 0;
    }
}
