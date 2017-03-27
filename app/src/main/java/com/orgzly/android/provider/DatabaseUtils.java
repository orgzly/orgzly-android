package com.orgzly.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orgzly.BuildConfig;
import com.orgzly.android.NotePosition;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteAncestor;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.LogUtils;

/**
 * Orgzly-specific {@link GenericDatabaseUtils}.
 */
public class DatabaseUtils {
    private static final String TAG = DatabaseUtils.class.getName();

    public static final String[] PROJECTION_FOR_ID = new String[] { "_id" };
    public static final String[] PROJECTION_FOR_COUNT = new String[] { "count(*)" };

    public static final String WHERE_EXISTING_NOTES = "(" +
                                                      DbNote.Column.IS_CUT + " = 0" + " AND " +
                                                      DbNote.Column.LEVEL + " > 0" + ")"; // Root note is a dummy note with level 0


    public static final String WHERE_VISIBLE_NOTES = "(" +
                                                     WHERE_EXISTING_NOTES + " AND " +
                                                     GenericDatabaseUtils.whereNullOrZero(DbNote.Column.FOLDED_UNDER_ID) + ")";

    public static String whereUncutBookNotes(long bookId) {
        return "(" + DbNote.Column.BOOK_ID + " = " + bookId + " AND " + WHERE_EXISTING_NOTES + ")";
    }

    public static String whereAncestors(long bookId, long lft, long rgt) {
        return "(" + whereUncutBookNotes(bookId) + " AND " +
               DbNote.Column.LFT + "< " + lft + " AND " + rgt + " < " + DbNote.Column.RGT + ")";
    }

    public static String whereAncestors(long bookId, String ids) {
        String sql = "(" + DbNote.Column._ID + " IN (SELECT DISTINCT b." + DbNote.Column._ID + " FROM " +
                     DbNote.TABLE + " a, " + DbNote.TABLE + " b WHERE " +
                     "a." + DbNote.Column.BOOK_ID + " = " + bookId + " AND " +
                     "b." + DbNote.Column.BOOK_ID + " = " + bookId + " AND " +
                     "a." + DbNote.Column._ID + " IN (" + ids + ") AND " +
                     "b." + DbNote.Column.LFT + " < a." + DbNote.Column.LFT + " AND a." + DbNote.Column.RGT + " < b." + DbNote.Column.RGT + "))";

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "whereAncestors: " + sql);

        return sql;
    }

    public static String ancestorsIds(SQLiteDatabase db, long bookId, long noteId) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] { "group_concat(" + DbNote.Column._ID + ", ',')" },
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
               lft + " < " + DbNote.Column.LFT + " AND " + DbNote.Column.RGT + " < " + rgt + ")";
    }

    public static String whereDescendantsAndNote(long bookId, long lft, long rgt) {
        return "(" + whereUncutBookNotes(bookId) + " AND " +
               lft + " <= " + DbNote.Column.LFT + " AND " + DbNote.Column.RGT + " <= " + rgt + ")";
    }

    public static String whereDescendantsAndNotes(long bookId, String ids) {
        return DbNote.Column._ID + " IN (SELECT DISTINCT d." + DbNote.Column._ID + " FROM " +
               DbNote.TABLE + " n, " + DbNote.TABLE + " d WHERE " +
               "d." + DbNote.Column.BOOK_ID + " = " + bookId + " AND " +
               "n." + DbNote.Column.BOOK_ID + " = " + bookId + " AND " +
               "n." + DbNote.Column._ID + " IN (" + ids + ") AND " +
               "d." + DbNote.Column.IS_CUT + " = 0 AND " +
               "n." + DbNote.Column.LFT + " <= d." + DbNote.Column.LFT + " AND d." + DbNote.Column.RGT + " <= n." + DbNote.Column.RGT + ")";
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
        return DatabaseUtils.updateBookMtime(db, DbBook.Column._ID + "=" + bookId, null);
    }

    /**
     * Sets modification time for selected notebooks to now.
     */
    public static int updateBookMtime(SQLiteDatabase db, String where, String[] whereArgs) {
        ContentValues values = new ContentValues();

        values.put(DbBook.Column.MTIME, System.currentTimeMillis());

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
                selection = bookSelection + " AND " + DbNote.Column.LFT + " >= " + targetNotePosition.getLft();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.LFT);

                selection = bookSelection + " AND " + DbNote.Column.RGT + " > " + targetNotePosition.getLft();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.RGT);

                lft = targetNotePosition.getLft();
                level = targetNotePosition.getLevel();

                break;

            case UNDER:
                selection = bookSelection + " AND " + DbNote.Column.LFT + " > " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.LFT);

                selection = bookSelection + " AND " + DbNote.Column.RGT + " >= " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.RGT);

                lft = targetNotePosition.getRgt();
                level = targetNotePosition.getLevel() + 1;

                break;

            case BELOW:
                selection = bookSelection + " AND " + DbNote.Column.LFT + " > " + targetNotePosition.getRgt();

                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection,
                        spaceRequired, ProviderContract.Notes.UpdateParam.LFT);

                // Container notes - update their RGT.
                selection = bookSelection + " AND " + DbNote.Column.RGT + " > " + targetNotePosition.getRgt();

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
                new String[] { DbNote.Column._ID, DbNote.Column.LFT, DbNote.Column.RGT, DbNote.Column.BOOK_ID },
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
                             " SET " + DbNote.Column.DESCENDANTS_COUNT + " = " + descendantsCount +
                             " WHERE " + DbNote.Column._ID + " = " + id;

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
                DbNote.Column.LFT + " < " + n.getLft() + " AND " +
                DbNote.Column.PARENT_ID + " = " + n.getParentId(),
                null,
                null,
                null,
                DbNote.Column.LFT + " DESC");
        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }

        return 0;
    }

    public static void updateParentIds(SQLiteDatabase db, long bookId) {
        long t = System.currentTimeMillis();

        String parentId = "(SELECT " + DbNote.Column._ID + " FROM " + DbNote.TABLE + " AS n WHERE " +
                          DbNote.Column.BOOK_ID + " = " + bookId + " AND " +
                          "n." + DbNote.Column.LFT + " < " + DbNote.TABLE + "." + DbNote.Column.LFT + " AND " +
                          DbNote.TABLE + "." + DbNote.Column.RGT + " < n." + DbNote.Column.RGT + " ORDER BY n." + DbNote.Column.LFT + " DESC LIMIT 1)";

        db.execSQL("UPDATE " + DbNote.TABLE + " SET " + DbNote.Column.PARENT_ID + " = " + parentId + " WHERE " + whereUncutBookNotes(bookId));

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "" + (System.currentTimeMillis() - t) + "ms");
    }

    public static void updateNoteAncestors(SQLiteDatabase db, long bookId) {
        long t = System.currentTimeMillis();

        db.execSQL("DELETE FROM " + DbNoteAncestor.TABLE + " WHERE " + DbNoteAncestor.Column.BOOK_ID + " = " + bookId);

        db.execSQL("INSERT INTO " + DbNoteAncestor.TABLE + " (book_id, note_id, ancestor_note_id) " +
                   "SELECT n." + DbNote.Column.BOOK_ID + ", n." + DbNote.Column._ID + ", a." + DbNote.Column._ID + " FROM " + DbNote.TABLE + " n " +
                   "JOIN " + DbNote.TABLE + " a ON (n." + DbNote.Column.BOOK_ID + " = a." + DbNote.Column.BOOK_ID + " AND a." + DbNote.Column.LFT + " < n." + DbNote.Column.LFT + " AND n." + DbNote.Column.RGT + " < a." + DbNote.Column.RGT + ") " +
                   "WHERE n." + DbNote.Column.BOOK_ID + " = " + bookId + " AND a." + DbNote.Column.LEVEL + " > 0");

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Done for " + bookId + " in " + + (System.currentTimeMillis() - t) + " ms");
    }
}
