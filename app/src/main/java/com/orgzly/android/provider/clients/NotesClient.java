package com.orgzly.android.provider.clients;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.android.Note;
import com.orgzly.android.NotePosition;
import com.orgzly.android.NotesBatch;
import com.orgzly.android.provider.DatabaseUtils;
import com.orgzly.android.provider.GenericDatabaseUtils;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.views.DbNoteView;
import com.orgzly.android.query.Condition;
import com.orgzly.android.query.Query;
import com.orgzly.android.query.user.InternalQueryBuilder;
import com.orgzly.android.ui.NotePlace;
import com.orgzly.android.ui.NoteStates;
import com.orgzly.android.ui.Place;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.util.MiscUtils;
import com.orgzly.org.OrgHead;
import com.orgzly.org.OrgProperties;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class NotesClient {
    private static final String TAG = NotesClient.class.getName();

    public interface NotesClientInterface {
        void onNote(Note note);
    }

    public static void forEachBookNote(Context context, String bookName, NotesClientInterface notesClientInterface) {
        Cursor cursor = NotesClient.getCursorForBook(context, bookName);

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                Note note = NotesClient.fromCursor(cursor);

                OrgProperties propertiesFromCursor = getNoteProperties(context, note.getId());

                note.getHead().setProperties(propertiesFromCursor);

                notesClientInterface.onNote(note);
            }
        } finally {
            cursor.close();
        }
    }

    public static OrgProperties getNoteProperties(Context context, long noteId) {
        OrgProperties properties = new OrgProperties();

        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.NoteProperties.ContentUri.notesIdProperties(noteId), null, null, null, null)) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                properties.put(cursor.getString(0), cursor.getString(1));
            }
        }

        return properties;
    }

    public static void toContentValues(ContentValues values, Note note) {
        values.put(ProviderContract.Notes.UpdateParam.BOOK_ID, note.getPosition().getBookId());

        values.put(ProviderContract.Notes.UpdateParam.CREATED_AT, note.getCreatedAt());

        values.put(ProviderContract.Notes.UpdateParam.LFT, note.getPosition().getLft());
        values.put(ProviderContract.Notes.UpdateParam.RGT, note.getPosition().getRgt());
        values.put(ProviderContract.Notes.UpdateParam.LEVEL, note.getPosition().getLevel());
        values.put(ProviderContract.Notes.UpdateParam.IS_FOLDED, note.getPosition().isFolded());
        values.put(ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT, note.getPosition().getDescendantsCount());
        values.put(ProviderContract.Notes.UpdateParam.FOLDED_UNDER_ID, note.getPosition().getFoldedUnderId());

        values.put(ProviderContract.Notes.UpdateParam.POSITION, 0); // TODO: Remove

        toContentValues(values, note.getHead());
    }

    private static void toContentValues(ContentValues values, OrgHead head) {
        values.put(ProviderContract.Notes.UpdateParam.TITLE, head.getTitle());

        if (head.hasScheduled()) {
            values.put(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING, head.getScheduled().toString());
        } else {
            values.putNull(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
        }

        if (head.hasClosed()) {
            values.put(ProviderContract.Notes.UpdateParam.CLOSED_STRING, head.getClosed().toString());
        } else {
            values.putNull(ProviderContract.Notes.UpdateParam.CLOSED_STRING);
        }

        if (head.hasClock()) {
            values.put(ProviderContract.Notes.UpdateParam.CLOCK_STRING, head.getClock().toString());
        } else {
            values.putNull(ProviderContract.Notes.UpdateParam.CLOCK_STRING);
        }

        if (head.hasDeadline()) {
            values.put(ProviderContract.Notes.UpdateParam.DEADLINE_STRING, head.getDeadline().toString());
        } else {
            values.putNull(ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
        }

        values.put(ProviderContract.Notes.UpdateParam.PRIORITY, head.getPriority());
        values.put(ProviderContract.Notes.UpdateParam.STATE, head.getState());

        if (head.hasTags()) {
            values.put(ProviderContract.Notes.UpdateParam.TAGS, DbNote.dbSerializeTags(head.getTags()));
        } else {
            values.putNull(ProviderContract.Notes.UpdateParam.TAGS);
        }

        if (head.hasContent()) {
            values.put(ProviderContract.Notes.UpdateParam.CONTENT, head.getContent());
            values.put(ProviderContract.Notes.UpdateParam.CONTENT_LINE_COUNT, MiscUtils.lineCount(head.getContent()));
        } else {
            values.putNull(ProviderContract.Notes.UpdateParam.CONTENT);
            values.put(ProviderContract.Notes.UpdateParam.CONTENT_LINE_COUNT, 0);
        }
    }

    public static Note fromCursor(Cursor cursor) {
        return fromCursor(cursor, false);
    }

    public static Note fromCursor(Cursor cursor, boolean withExtras) {
        long id = idFromCursor(cursor);

        long createdAt = cursor.getLong(cursor.getColumnIndex(DbNoteView.CREATED_AT));

        int contentLines = cursor.getInt(cursor.getColumnIndex(DbNoteView.CONTENT_LINE_COUNT));

        OrgHead head = headFromCursor(cursor);

        NotePosition position = DbNote.positionFromCursor(cursor);

        Note note = new Note();

        note.setHead(head);
        note.setId(id);
        note.setCreatedAt(createdAt);
        note.setPosition(position);
        note.setContentLines(contentLines);

        if (withExtras) {
            String inheritedTags = cursor.getString(cursor.getColumnIndex(DbNoteView.INHERITED_TAGS));
            if (! TextUtils.isEmpty(inheritedTags)) {
                note.setInheritedTags(DbNote.dbDeSerializeTags(inheritedTags));
            }
        }

        return note;
    }

    public static long idFromCursor(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(DbNoteView._ID));
    }

    private static OrgHead headFromCursor(Cursor cursor) {
        OrgHead head = new OrgHead();

        String state = cursor.getString(cursor.getColumnIndex(DbNoteView.STATE));
        if (NoteStates.isKeyword(state)) {
            head.setState(state);
        } else {
            head.setState(null);
        }

        String priority = cursor.getString(cursor.getColumnIndex(DbNoteView.PRIORITY));
        if (priority != null) {
            head.setPriority(priority);
        }

        head.setTitle(cursor.getString(cursor.getColumnIndex(DbNoteView.TITLE)));

        head.setContent(cursor.getString(cursor.getColumnIndex(DbNoteView.CONTENT)));

        if (! TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(DbNoteView.SCHEDULED_RANGE_STRING))))
            head.setScheduled(OrgRange.parse(cursor.getString(cursor.getColumnIndex(DbNoteView.SCHEDULED_RANGE_STRING))));
        if (! TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(DbNoteView.DEADLINE_RANGE_STRING))))
            head.setDeadline(OrgRange.parse(cursor.getString(cursor.getColumnIndex(DbNoteView.DEADLINE_RANGE_STRING))));
        if (! TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(DbNoteView.CLOSED_RANGE_STRING))))
            head.setClosed(OrgRange.parse(cursor.getString(cursor.getColumnIndex(DbNoteView.CLOSED_RANGE_STRING))));
        if (! TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(DbNoteView.CLOCK_RANGE_STRING))))
            head.setClock(OrgRange.parse(cursor.getString(cursor.getColumnIndex(DbNoteView.CLOCK_RANGE_STRING))));

        // TODO: This is probably slowing UI down when scrolling fast, use strings from db directly?
        String tags = cursor.getString(cursor.getColumnIndex(DbNoteView.TAGS));
        if (! TextUtils.isEmpty(tags)) {
            head.setTags(DbNote.dbDeSerializeTags(tags));
        }

        return head;
    }

    /**
     * Updates note by its ID.
     */
    public static int update(Context context, Note note) {
        ContentValues values = new ContentValues();
        toContentValues(values, note);

        Uri noteUri = ProviderContract.Notes.ContentUri.notesId(note.getId());
        Uri uri = noteUri.buildUpon().appendQueryParameter("bookId", String.valueOf(note.getPosition().getBookId())).build();

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        /* Update note. */
        ops.add(ContentProviderOperation
                .newUpdate(uri)
                .withValues(values)
                .build()
        );

        /* Delete all note's properties. */
        ops.add(ContentProviderOperation
                .newDelete(ProviderContract.NoteProperties.ContentUri.notesIdProperties(note.getId()))
                .build()
        );

        /* Add each of the note's property. */
        int i = 0;
        OrgProperties properties = note.getHead().getProperties();
        for (String name: properties.keySet()) {
            String value = properties.get(name);

            values = new ContentValues();

            values.put(ProviderContract.NoteProperties.Param.NOTE_ID, note.getId());
            values.put(ProviderContract.NoteProperties.Param.NAME, name);
            values.put(ProviderContract.NoteProperties.Param.VALUE, value);
            values.put(ProviderContract.NoteProperties.Param.POSITION, i++);

            ops.add(ContentProviderOperation
                    .newInsert(ProviderContract.NoteProperties.ContentUri.notesProperties())
                    .withValues(values)
                    .build()
            );
        }

        ContentProviderResult[] result;

        try {
            result = context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return result[0].count;
    }

    public static int updateContent(Context context, long bookId, long noteId, String content) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ContentValues values = new ContentValues();
        values.put(DbNote.CONTENT, content);

        // Update content
        ops.add(ContentProviderOperation
                .newUpdate(ProviderContract.Notes.ContentUri.notes())
                .withValues(values)
                .withSelection(DbNote._ID + " = " + noteId, null)
                .build()
        );

        // Update book's modification time
        ops.add(ContentProviderOperation
                .newUpdate(ProviderContract.Books.ContentUri.books())
                .withValue(DbBook.MTIME, System.currentTimeMillis())
                .withSelection(DbBook._ID + " = " + bookId, null)
                .build());

        ContentProviderResult[] result;

        try {
            result = context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return result[0].count;
    }

    public static Note create(Context context, Note note) {
        return create(context, note, null, note.getCreatedAt());
    }

    /**
     * Insert as last note if position is not specified.
     */
    public static Note create(Context context, Note note, NotePlace target, long time) {
        ContentValues values = new ContentValues();
        toContentValues(values, note);

        Uri insertUri;

        if (target != null) {
            /* Create note relative to an existing note. */
            insertUri = ProviderContract.Notes.ContentUri.notesIdTarget(target);
        } else {
            /* Create as last note. */
            insertUri = ProviderContract.Notes.ContentUri.notes();
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        /* Insert note. */
        ops.add(ContentProviderOperation
                .newInsert(insertUri)
                .withValues(values)
                .build()
        );

        /* Add each of the note's property. */
        int i = 0;
        OrgProperties properties = note.getHead().getProperties();
        for (String name: properties.keySet()) {
            String value = properties.get(name);

            values = new ContentValues();

            values.put(ProviderContract.NoteProperties.Param.NAME, name);
            values.put(ProviderContract.NoteProperties.Param.VALUE, value);
            values.put(ProviderContract.NoteProperties.Param.POSITION, i++);

            ops.add(ContentProviderOperation
                    .newInsert(ProviderContract.NoteProperties.ContentUri.notesProperties())
                    .withValues(values)
                    .withValueBackReference(ProviderContract.NoteProperties.Param.NOTE_ID, 0)
                    .build()
            );
        }

        // Update book's modification time
        ops.add(ContentProviderOperation
                .newUpdate(ProviderContract.Books.ContentUri.books())
                .withValue(DbBook.MTIME, time)
                .withSelection(DbBook._ID + " = " + note.getPosition().getBookId(), null)
                .build());


        ContentProviderResult[] result;

        try {
            result = context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        long noteId = ContentUris.parseId(result[0].uri);

        /* Update ID of newly inserted note. */
        note.setId(noteId);

        return note;
    }

    /**
     * Deletes all notes belonging to specified book.
     */
    public static void deleteFromBook(Context context, long bookId) {
        int deleted = context.getContentResolver().delete(ProviderContract.Notes.ContentUri.notes(), ProviderContract.Notes.UpdateParam.BOOK_ID + "=" + bookId, null);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Deleted all (" + deleted + ") notes from book " + bookId);
    }

    public static int delete(Context context, long[] noteIds) {
        int deleted = 0;

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        for (long noteId: noteIds) {
            ops.add(ContentProviderOperation
                    .newDelete(ProviderContract.Notes.ContentUri.notes())
                    .withSelection(ProviderContract.Notes.UpdateParam._ID + "=" + noteId, null)
                    .build()
            );
        }

        try {
            context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Deleted " + deleted + " notes");

        return deleted;
    }

    /*
     * Pastes back the latest cut batch.
     * @return number of notes restored
     *
     * Restores all notes cut with specified batch (see cut())
     * @return number of notes restored
     */
//    public int undoCut() {
//        NotesBatch batch = getLatestNotesBatch();
//
//        if (batch == null) { // No cut notes.
//            return 0;
//        }
//
//        if (batch.getId() <= 0) {
//            throw new IllegalArgumentException("Paste batch id (" + batch + ") must be greater then 0");
//        }
//
//        if (batch.getId() > System.currentTimeMillis()) {
//            throw new IllegalArgumentException("Paste batch id (" + batch + ") must be less then current time");
//        }
//
//        int pasted = 0;
//
//        ContentValues values = new ContentValues();
//        values.put(Contract.Notes.IS_CUT, 0);
//
//        pasted += context.getContentResolver().update(Contract.Notes.CONTENT_URI, values, Contract.Notes.IS_CUT + "=" + batch.getId(), null);
//
//        if (BuildConfig.LOG_DEBUG) Dlog.d(TAG, "Pasted " + pasted + " notes with " + batch);
//
//
//        return pasted;
//    }

    /**
     * Get {@link Note} by id
     */
    public static Note getNote(Context context, long noteId) {
        Uri uri = ProviderContract.Notes.ContentUri.notes();
        String selection = DbNoteView._ID + "=" + noteId;

        // TODO: Do not select all columns, especially not content if not required.
        try (Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor);
            } else {
                Log.e(TAG, "Note with id " + noteId + " was not found in " + uri);
                return null;
            }
        }
    }

    /**
     * Returns first note that matches the title.
     * Currently used only by tests -- title is not unique and notebook ID is not even specified.
     */
    public static Note getNote(Context context, String title, boolean withExtras) {
        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notesWithExtras(), null, DbNoteView.TITLE + "= ?", new String[]{title}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor, withExtras);
            } else {
                throw new NoSuchElementException("Note with title " + title + " was not found in " + ProviderContract.Notes.ContentUri.notes());
            }
        }
    }

    public static Note getRootNode(Context context, long bookId) {
        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(), null, DbNoteView.BOOK_ID + "=" + bookId + " AND " + DbNoteView.LEVEL + "= 0", null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor, false);
            } else {
                throw new IllegalStateException("Book " + bookId + " has no root node");
            }
        }
    }

    public static List<Long[]> getNotesWithProperty(Context context, String propName, String propValue) {
        List<Long[]> results = new ArrayList<>();

        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notesWithProperty(propName, propValue), null, null, null, null)) {
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    results.add(new Long[] { cursor.getLong(0), cursor.getLong(1) });
                }
            }
        }

        return results;
    }

    public static CursorLoader getLoaderForQuery(Context context, String query) throws SQLException {
        return new CursorLoader(
                context,
                ProviderContract.Notes.ContentUri.notesSearchQueried(query),
                null,
                null,
                null,
                null);
    }

    public static Cursor getCursorForQuery(Context context, String query) throws SQLException {
        return context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notesSearchQueried(query),
                null,
                null,
                null,
                null);
    }

    public static Cursor getCursorForBook(Context context, String bookName) throws SQLException {
        /* Create a query with a book name condition. */
        InternalQueryBuilder builder = new InternalQueryBuilder();
        String query = builder.build(new Query(new Condition.InBook(bookName)));

        return context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notesSearchQueried(query),
                null,
                null,
                null,
                DbNoteView.LFT); // For book view, force ordering by position only
    }

    public static int getCount(Context context, Long bookId) {
        String selection;

        if (bookId != null) {
            selection = DatabaseUtils.whereUncutBookNotes(bookId);
        } else {
            selection = DatabaseUtils.WHERE_EXISTING_NOTES;
        }

        return GenericDatabaseUtils.getCount(context, ProviderContract.Notes.ContentUri.notes(), selection);
    }

    /**
     * Collects all known tags from database.
     * @return Array of all known tags
     */
    public static String[] getAllTags(Context context, long bookId) {
        Set<String> result = new HashSet<>();

        /* If book id is specified, return only tags from that book. */
        String selection = null;
        if (bookId > 0) {
            selection = DbNoteView.BOOK_ID + " = " + bookId;
        }

        Cursor cursor = context.getContentResolver().query(ProviderContract.Notes.ContentUri.notes(), new String[] { "DISTINCT " + DbNoteView.TAGS }, selection, null, null);
        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String tags = cursor.getString(0);
                if (! TextUtils.isEmpty(tags)) {
                    result.addAll(Arrays.asList(DbNote.dbDeSerializeTags(tags)));
                }
            }
        } finally {
            cursor.close();
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Get the id of the first note in book.
     *
     * Used only by tests.
     *
     * @return note id or 0 if none found
     */
    public static long getFirstNoteId(Context context, long bookId) {
        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(),
                DatabaseUtils.PROJECTION_FOR_ID,
                DatabaseUtils.whereUncutBookNotes(bookId),
                null,
                DbNoteView.LFT
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            } else { // No records found.
                return 0;
            }
        }
    }

    public static int cut(Context context, long bookId, Set<Long> noteIds) {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.Cut.Param.BOOK_ID, bookId);
        values.put(ProviderContract.Cut.Param.IDS, TextUtils.join(",", noteIds));

        return context.getContentResolver().update(ProviderContract.Cut.ContentUri.cut(), values, null, null);
    }

    public static NotesBatch paste(Context context, long bookId, long noteId, Place place) {
        NotesBatch batch = getLatestNotesBatch(context);

        if (batch != null) {
            ContentValues values = new ContentValues();

            values.put(ProviderContract.Paste.Param.SPOT, place.toString());
            values.put(ProviderContract.Paste.Param.NOTE_ID, noteId);
            values.put(ProviderContract.Paste.Param.BATCH_ID, batch.getId());

            context.getContentResolver().update(ProviderContract.Paste.ContentUri.paste(), values, null, null);
        }

        return batch;
    }

    public static int delete(Context context, long bookId, Set<Long> noteIds) {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.Delete.Param.BOOK_ID, bookId);
        values.put(ProviderContract.Delete.Param.IDS, TextUtils.join(",", noteIds));

        return context.getContentResolver().update(ProviderContract.Delete.ContentUri.delete(), values, null, null);
    }


    /**
     * Collect all notes with latest (newest, largest) batch id.
     * @return Latest {@link NotesBatch}
     */
    public static NotesBatch getLatestNotesBatch(Context context) {
        /* Get latest batch ID. */
        long batchId;
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(),
                new String[] { "MAX(" + DbNoteView.IS_CUT + ")" },
                null,
                null,
                null);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            batchId = cursor.getLong(0);
        } finally {
            cursor.close();
        }

        if (batchId == 0) {
            return null;
        }

        cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(),
                DatabaseUtils.PROJECTION_FOR_ID,
                DbNoteView.IS_CUT + " = " + batchId,
                null,
                null);
        try {
            int count = cursor.getCount();

            if (count == 0) {
                return null;
            }

            Set<Long> ids = new HashSet<>();

            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }

            NotesBatch batch = new NotesBatch(batchId, ids);

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Latest cut batch: " + batch);

            return batch;

        } finally {
            cursor.close();
        }
    }

    public static void updateScheduledTime(Context context, Set<Long> noteIds, OrgDateTime time) {
        updatePlanningTime(context, noteIds, time, ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
    }

    public static void updateDeadlineTime(Context context, Set<Long> noteIds, OrgDateTime time) {
        updatePlanningTime(context, noteIds, time, ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
    }

    private static void updatePlanningTime(Context context, Set<Long> noteIds, OrgDateTime time, String key) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        String noteIdsCommaSeparated = TextUtils.join(",", noteIds);

        /* Update notes. */
        ContentValues values = new ContentValues();

        if (time != null) {
            values.put(key, new OrgRange(time).toString());
        } else {
            values.putNull(key);
        }

        ops.add(ContentProviderOperation
                .newUpdate(ProviderContract.Notes.ContentUri.notes())
                .withValues(values)
                .withSelection(ProviderContract.Notes.UpdateParam._ID + " IN (" + noteIdsCommaSeparated + ")", null)
                .build());

        updateBooksMtimeForNotes(context, noteIdsCommaSeparated, ops);

        /*
         * Apply batch.
         */
        try {
            context.getContentResolver().applyBatch(ProviderContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Update state of specified notes.
     */
    public static void setState(Context context, Set<Long> noteIds, String state) {
        String noteIdsCommaSeparated = TextUtils.join(",", noteIds);

        ContentValues values = new ContentValues();
        values.put(ProviderContract.NotesState.Param.NOTE_IDS, noteIdsCommaSeparated);

        /*
         * TODO: Do not update state in DB with NO_STATE_KEYWORD - that should be UI-only thing
         * Then stop checking for it with NoteStates.isKeyword()
         */
        values.put(ProviderContract.NotesState.Param.STATE, state != null ? state : NoteStates.NO_STATE_KEYWORD);

        context.getContentResolver().update(ProviderContract.NotesState.ContentUri.notesState(), values, null, null);

        /* Affected books' mtime will be modified in content provider. */
    }

    /**
     * TODO: Add operation for updating books' mtime.
     * Make sure this is called after updating book's notes, as it will trigger book loader
     * which could load old notes if they were not already updated.
     */
    private static void updateBooksMtimeForNotes(Context context, String noteIdsCommaSeparated, ArrayList<ContentProviderOperation> ops) {
        String bookIdsCommaSeparated = getBooksForNotes(context, noteIdsCommaSeparated);

        if (bookIdsCommaSeparated != null) {
            ContentValues values = new ContentValues();
            values.put(ProviderContract.Books.Param.MTIME, System.currentTimeMillis());

            ops.add(ContentProviderOperation
                    .newUpdate(ProviderContract.Books.ContentUri.books())
                    .withValues(values)
                    .withSelection(ProviderContract.Books.Param._ID + " IN (" + bookIdsCommaSeparated + ")", null)
                    .build());
        }
    }

    /**
     * Get comma-separated list of distinct book ids for specified notes.
     */
    private static String getBooksForNotes(Context context, String noteIdsCommaSeparated) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(),
                new String[] { "GROUP_CONCAT(DISTINCT " + DbNoteView.BOOK_ID + ")" },
                DbNoteView._ID + " IN (" + noteIdsCommaSeparated + ")",
                null,
                null);

        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    public static List<Long> getDescendantsIds(Context context, Note note) {
        List<Long> ids = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Notes.ContentUri.notes(),
                new String[] { DbNoteView._ID },
                DatabaseUtils.whereDescendants(
                        note.getPosition().getBookId(),
                        note.getPosition().getLft(),
                        note.getPosition().getRgt()),
                null,
                null);

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                long id = cursor.getLong(0);

                ids.add(id);
            }

        } finally {
            cursor.close();
        }

        return ids;
    }

    public static void toggleFoldedState(Context context, long noteId) {
        context.getContentResolver().update(ProviderContract.Notes.ContentUri.notesIdToggleFoldedState(noteId), null, null, null);
    }
}
