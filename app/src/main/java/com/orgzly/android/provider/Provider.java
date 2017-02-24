package com.orgzly.android.provider;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.RenamingDelegatingContext;
import android.text.TextUtils;
import android.util.Log;

import com.orgzly.BuildConfig;
import com.orgzly.android.Note;
import com.orgzly.android.NotePosition;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.StateChangeLogic;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.actions.Action;
import com.orgzly.android.provider.actions.ActionRunner;
import com.orgzly.android.provider.actions.CutNotesAction;
import com.orgzly.android.provider.actions.CycleVisibilityAction;
import com.orgzly.android.provider.actions.DeleteNotesAction;
import com.orgzly.android.provider.actions.DemoteNotesAction;
import com.orgzly.android.provider.actions.MoveNotesAction;
import com.orgzly.android.provider.actions.PasteNotesAction;
import com.orgzly.android.provider.actions.PromoteNotesAction;
import com.orgzly.android.provider.actions.SparseTreeAction;
import com.orgzly.android.provider.actions.ToggleFoldedStateAction;
import com.orgzly.android.provider.clients.BooksClient;
import com.orgzly.android.provider.clients.NotesClient;
import com.orgzly.android.provider.models.DbBook;
import com.orgzly.android.provider.models.DbBookLink;
import com.orgzly.android.provider.models.DbBookSync;
import com.orgzly.android.provider.models.DbCurrentVersionedRook;
import com.orgzly.android.provider.models.DbDbRepo;
import com.orgzly.android.provider.models.DbNote;
import com.orgzly.android.provider.models.DbNoteProperty;
import com.orgzly.android.provider.models.DbOrgRange;
import com.orgzly.android.provider.models.DbOrgTimestamp;
import com.orgzly.android.provider.models.DbProperty;
import com.orgzly.android.provider.models.DbPropertyName;
import com.orgzly.android.provider.models.DbPropertyValue;
import com.orgzly.android.provider.models.DbRepo;
import com.orgzly.android.provider.models.DbRook;
import com.orgzly.android.provider.models.DbRookUrl;
import com.orgzly.android.provider.models.DbSearch;
import com.orgzly.android.provider.models.DbVersionedRook;
import com.orgzly.android.provider.views.BooksView;
import com.orgzly.android.provider.views.NotesView;
import com.orgzly.android.ui.Placement;
import com.orgzly.android.util.EncodingDetect;
import com.orgzly.android.util.LogUtils;
import com.orgzly.org.OrgFile;
import com.orgzly.org.OrgProperty;
import com.orgzly.org.datetime.OrgDateTime;
import com.orgzly.org.datetime.OrgRange;
import com.orgzly.org.parser.OrgNestedSetParserListener;
import com.orgzly.org.parser.OrgNode;
import com.orgzly.org.parser.OrgNodeInSet;
import com.orgzly.org.parser.OrgParser;
import com.orgzly.org.parser.OrgParserWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Provider extends ContentProvider {
    private static final String TAG = Provider.class.getName();

    protected Database mOpenHelper;

    private static final ProviderUris uris = new ProviderUris();

    @Override
    public boolean onCreate() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new Database(getContext());

        return true;
    }

    @Override
    public String getType(Uri uri) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString());
        return null;
    }

    /**
     * Apply operations under single transaction.
     */
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            ContentProviderResult[] results = super.applyBatch(operations);
            db.setTransactionSuccessful();
            return results;

        } finally {
            db.endTransaction();
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), projection, selection, selectionArgs, sortOrder);

        /* Gets a readable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        long id;
        String table;

        switch (uris.matcher.match(uri)) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                table = DbRepo.TABLE;
                selection = DbRepo.Column.IS_REPO_ACTIVE + "= 1";
                selectionArgs = null;
                break;

            case ProviderUris.REPOS_ID:
                table = DbRepo.TABLE;
                selection = DbRepo.Column._ID + "=?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;

            case ProviderUris.NOTES:
                table = NotesView.VIEW_NAME;
                break;

            case ProviderUris.NOTES_SEARCH_QUERIED:
                Cursor cursor = queryNotesSearchQueried(db, uri.getQuery(), sortOrder);

                // Search results depend on notes and notebooks
                cursor.setNotificationUri(getContext().getContentResolver(), ProviderContract.Notes.ContentUri.notes());
                cursor.setNotificationUri(getContext().getContentResolver(), ProviderContract.Books.ContentUri.books());

                return cursor;

            case ProviderUris.BOOKS_ID_NOTES:
                table = NotesView.VIEW_NAME;

                id = Long.parseLong(uri.getPathSegments().get(1));

                selection = NotesView.Columns.BOOK_ID + "=" + id + " AND " + DatabaseUtils.WHERE_VISIBLE_NOTES;
                selectionArgs = null;

                uri = ProviderContract.Notes.ContentUri.notes();
                break;

            case ProviderUris.NOTES_ID_PROPERTIES:
                id = Long.parseLong(uri.getPathSegments().get(1));

                selection = DbNoteProperty.TABLE + "." + DbNoteProperty.Column.NOTE_ID + "=" + id;
                selectionArgs = null;

                sortOrder = DbNoteProperty.Column.POSITION;


                table = DbNoteProperty.TABLE + " " +
                        GenericDatabaseUtils.join(DbProperty.TABLE, "tproperties", DbProperty._ID, DbNoteProperty.TABLE, DbNoteProperty.Column.PROPERTY_ID) +
                        GenericDatabaseUtils.join(DbPropertyName.TABLE, "tpropertyname", DbPropertyName._ID, "tproperties", DbProperty.Column.NAME_ID) +
                        GenericDatabaseUtils.join(DbPropertyValue.TABLE, "tpropertyvalue", DbPropertyValue._ID, "tproperties", DbProperty.Column.VALUE_ID);

                projection = new String[] {
                        "tpropertyname." + DbPropertyName.Column.NAME,
                        "tpropertyvalue." + DbPropertyValue.Column.VALUE,
                };

                break;

            case ProviderUris.BOOKS:
                table = BooksView.VIEW_NAME;
                break;

            case ProviderUris.BOOKS_ID:
                table = BooksView.VIEW_NAME;
                selection = DbBook.Column._ID + "=?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                break;

            case ProviderUris.FILTERS_ID:
                table = DbSearch.TABLE;
                selection = DbSearch.Column._ID + "=?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;

            case ProviderUris.CURRENT_ROOKS:
                projection = new String[] {
                        DbRepo.TABLE + "." + DbRepo.Column.REPO_URL,
                        DbRookUrl.TABLE + "." + DbRookUrl.Column.ROOK_URL,
                        DbVersionedRook.TABLE + "." + DbVersionedRook.Column.ROOK_REVISION,
                        DbVersionedRook.TABLE + "." + DbVersionedRook.Column.ROOK_MTIME,
                };

                table = DbCurrentVersionedRook.TABLE +
                        " LEFT JOIN " + DbVersionedRook.TABLE + " ON (" + DbVersionedRook.TABLE + "." + DbVersionedRook.Column._ID + "=" + DbCurrentVersionedRook.TABLE + "." + DbCurrentVersionedRook.Column.VERSIONED_ROOK_ID + ")" +
                        " LEFT JOIN " + DbRook.TABLE + " ON (" + DbRook.TABLE + "." + DbRook.Column._ID + "=" + DbVersionedRook.TABLE + "." + DbVersionedRook.Column.ROOK_ID + ")" +
                        " LEFT JOIN " + DbRookUrl.TABLE + " ON (" + DbRookUrl.TABLE + "." + DbRookUrl.Column._ID + "=" + DbRook.TABLE + "." + DbRook.Column.ROOK_URL_ID + ")" +
                        " LEFT JOIN " + DbRepo.TABLE + " ON (" + DbRepo.TABLE + "." + DbRepo.Column._ID + "=" + DbRook.TABLE + "." + DbRook.Column.REPO_ID + ")" +
                        "";
                break;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        Cursor cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Cursor count: " + cursor.getCount() + " for " + table + " " + selection + " " + selectionArgs);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Builds query parameters from {@link SearchQuery}.
     */
    private Cursor queryNotesSearchQueried(SQLiteDatabase db, String query, String sortOrder) {
        SearchQuery searchQuery = new SearchQuery(query);

        String table;
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();

        table = NotesView.VIEW_NAME;

        if (searchQuery.hasTags()) {
            List<String> whereTags = new ArrayList<>();

            /*
             * We are only searching for a tag within a string of tags.
             * "tag" will be found in "few tagy ones"
             */
            for (String tag: searchQuery.getTags()) {
                whereTags.add("n1." + DbNote.Column.TAGS + " LIKE ?");
                selectionArgs.add("%" + tag + "%");
            }

            table = "(select n2.* from " + NotesView.VIEW_NAME +
                    " n1 LEFT OUTER JOIN " + NotesView.VIEW_NAME +
                    " n2 WHERE " + TextUtils.join(" AND ", whereTags) + " AND " +
                    " n1." + DbNote.Column.BOOK_ID + " = n2." + DbNote.Column.BOOK_ID + " AND " +
                    " n1." + DbNote.Column.IS_CUT + " = 0 AND n2." + DbNote.Column.IS_CUT + " = 0 AND " +
                    "(n1." + DbNote.Column.LFT + " <= n2." + DbNote.Column.LFT +
                    " and n2." + DbNote.Column.RGT + " <= n1." + DbNote.Column.RGT + ") GROUP BY n2._id) n";
        }

        /* Skip cut notes. */
        selection.append(DatabaseUtils.WHERE_EXISTING_NOTES);

        if (searchQuery.hasBookName()) {
            selection.append(" AND ").append(ProviderContract.Notes.QueryParam.BOOK_NAME).append(" = ?");
            selectionArgs.add(searchQuery.getBookName());
        }

        if (searchQuery.hasNotBookName()) {
            for (String name: searchQuery.getNotBookName()) {
                selection.append(" AND ").append(ProviderContract.Notes.QueryParam.BOOK_NAME).append(" != ?");
                selectionArgs.add(name);
            }
        }

        if (searchQuery.hasState()) {
            selection.append(" AND COALESCE(" + ProviderContract.Notes.QueryParam.STATE + ", '') = ?");
            selectionArgs.add(searchQuery.getState());
        }

        if (searchQuery.hasNotState()) {
            for (String state: searchQuery.getNotState()) {
                selection.append(" AND COALESCE(" + ProviderContract.Notes.QueryParam.STATE + ", '') != ?");
                selectionArgs.add(state);
            }
        }

        for (String token: searchQuery.getTextSearch()) {
            selection.append(" AND (" + ProviderContract.Notes.QueryParam.TITLE + " LIKE ?");
            selectionArgs.add("%" + token + "%");
            selection.append(" OR " + ProviderContract.Notes.QueryParam.CONTENT + " LIKE ?");
            selectionArgs.add("%" + token + "%");
            selection.append(" OR " + ProviderContract.Notes.QueryParam.TAGS + " LIKE ?");
            selectionArgs.add("%" + token + "%");
            selection.append(")");
        }

        if (searchQuery.hasScheduled()) {
            appendBeforeInterval(selection, ProviderContract.Notes.QueryParam.SCHEDULED_TIME_TIMESTAMP, searchQuery.getScheduled());
        }

        if (searchQuery.hasDeadline()) {
            appendBeforeInterval(selection, ProviderContract.Notes.QueryParam.DEADLINE_TIME_TIMESTAMP, searchQuery.getDeadline());
        }

        /*
         * Handle empty string and NULL - use default priority in those cases.
         * lower( coalesce( nullif(PRIORITY, ''), DEFAULT) )
         */
        if (searchQuery.hasPriority()) {
            String defaultPriority = AppPreferences.defaultPriority(getContext());
            selection.append(" AND lower(coalesce(nullif(" + ProviderContract.Notes.QueryParam.PRIORITY + ", ''), ?)) = ?");
            selectionArgs.add(defaultPriority);
            selectionArgs.add(searchQuery.getPriority());
        }

        if (searchQuery.hasNoteTags()) {
            /*
             * We are only searching for a tag within a string of tags.
             * "tag" will be found in "few tagy ones"
             * Tags must be kept separately so we can match them exactly.
             */
            for (String tag: searchQuery.getNoteTags()) {
                selection.append(" AND " + ProviderContract.Notes.QueryParam.TAGS + " LIKE ?");
                selectionArgs.add("%" + tag + "%");
            }
        }

        String sql = "SELECT * FROM " + table + " WHERE " + selection.toString() + " ORDER BY " + sortOrder;

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sql, selectionArgs);
        return db.rawQuery(sql, selectionArgs.toArray(new String[selectionArgs.size()]));
    }

    private static void appendBeforeInterval(StringBuilder selection, String column, SearchQuery.SearchQueryInterval interval) {
        Calendar before = new GregorianCalendar();

        switch (interval.getUnit()) {
            case DAY:
                before.add(Calendar.DAY_OF_MONTH, interval.getValue());
                break;
            case WEEK:
                before.add(Calendar.WEEK_OF_YEAR, interval.getValue());
                break;
            case MONTH:
                before.add(Calendar.MONTH, interval.getValue());
                break;
            case YEAR:
                before.add(Calendar.YEAR, interval.getValue());
                break;
        }

        /* Add one more day, as we use less-then operator. */
        before.add(Calendar.DAY_OF_MONTH, 1);

        /* 00:00 */
        before.set(Calendar.HOUR_OF_DAY, 0);
        before.set(Calendar.MINUTE, 0);
        before.set(Calendar.SECOND, 0);
        before.set(Calendar.MILLISECOND, 0);

        selection
                .append(" AND ").append(column).append(" != 0")
                .append(" AND ").append(column).append(" < ").append(before.getTimeInMillis());
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), values);

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        Set<Uri> notifyUris = new HashSet<>();
        notifyUris.add(uri);

        db.beginTransaction();
        try {
            for (int i = 0; i < values.length; i++) {
                insertUnderTransaction(db, notifyUris, uri, values[i]);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        for (Uri notifyUri: notifyUris) {
            notifyChange(getContext(), notifyUri);
        }

        return values.length;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), contentValues);

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        Uri resultUri;

        Set<Uri> notifyUris = new HashSet<>();
        notifyUris.add(uri); // FIXME: This notifies for urls such as content://com.orgzly/load-from-file

        db.beginTransaction();
        try {
            resultUri = insertUnderTransaction(db, notifyUris, uri, contentValues);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        for (Uri notifyUri: notifyUris) {
            notifyChange(getContext(), notifyUri);
        }

        return resultUri;
    }

    private Uri insertUnderTransaction(SQLiteDatabase db, Set<Uri> notifyUris, Uri uri, ContentValues contentValues) {
        long id, noteId;
        String table;
        Uri resultUri;

        switch (uris.matcher.match(uri)) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                id = DbRepo.insert(db, notifyUris, contentValues.getAsString(ProviderContract.Repos.Param.REPO_URL));
                return ContentUris.withAppendedId(uri, id);

            case ProviderUris.BOOKS:
                table = DbBook.TABLE;
                long bid = db.insertOrThrow(table, null, contentValues);
                insertRootNote(db, bid);
                return ContentUris.withAppendedId(uri, bid);

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                ProviderFilters.updateWithNextPosition(db, contentValues);
                break;

            case ProviderUris.NOTES:
                return insertNote(db, uri, contentValues, Placement.UNDEFINED);

            case ProviderUris.NOTE_ABOVE:
                return insertNote(db, uri, contentValues, Placement.ABOVE);

            case ProviderUris.NOTE_UNDER:
                return insertNote(db, uri, contentValues, Placement.UNDER);

            case ProviderUris.NOTE_BELOW:
                return insertNote(db, uri, contentValues, Placement.BELOW);

            case ProviderUris.NOTES_PROPERTIES:
                noteId = contentValues.getAsLong(ProviderContract.NoteProperties.Param.NOTE_ID);
                String name = contentValues.getAsString(ProviderContract.NoteProperties.Param.NAME);
                String value = contentValues.getAsString(ProviderContract.NoteProperties.Param.VALUE);
                int position = contentValues.getAsInteger(ProviderContract.NoteProperties.Param.POSITION);

                id = new DbNoteProperty(
                        noteId,
                        position,
                        new DbProperty(new DbPropertyName(name), new DbPropertyValue(value))
                ).save(db);

                return ContentUris.withAppendedId(uri, id);

            case ProviderUris.LOAD_BOOK_FROM_FILE:
                resultUri = loadBookFromFile(contentValues);

                notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                notifyUris.add(ProviderContract.Books.ContentUri.books());

                return resultUri;

            case ProviderUris.CURRENT_ROOKS:
                resultUri = insertCurrentRook(db, uri, contentValues);
                notifyUris.add(ProviderContract.Books.ContentUri.books());
                return resultUri;

            case ProviderUris.BOOKS_ID_SAVED:
                resultUri = bookSavedToRepo(db, uri, contentValues);
                notifyUris.add(ProviderContract.Books.ContentUri.books());
                return resultUri;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        id = db.insertOrThrow(table, null, contentValues);

        return ContentUris.withAppendedId(uri, id);
    }

    private long insertRootNote(SQLiteDatabase db, long bookId) {
        Note rootNote = Note.newRootNote(bookId);

        ContentValues values = new ContentValues();
        NotesClient.toContentValues(values, rootNote);
        replaceTimestampRangeStringsWithIds(db, values);

        return db.insertOrThrow(DbNote.TABLE, null, values);
    }

    private Uri insertNote(SQLiteDatabase db, Uri uri, ContentValues values, Placement placement) {
        NotePosition notePos = new NotePosition();

        long bookId = values.getAsLong(ProviderContract.Notes.UpdateParam.BOOK_ID);

        /* If new note is inserted relative to some other note, get info about that target note. */
        long refNoteId = 0;
        NotePosition refNotePos = null;
        if (placement != Placement.UNDEFINED) {
            refNoteId = Long.valueOf(uri.getPathSegments().get(1));
            refNotePos = DbNote.getPosition(db, refNoteId);
        }

        switch (placement) {
            case ABOVE:
                notePos.setLevel(refNotePos.getLevel());
                notePos.setLft(refNotePos.getLft());
                notePos.setRgt(refNotePos.getLft() + 1);
                notePos.setParentId(refNotePos.getParentId());

                break;

            case UNDER:
                notePos.setLevel(refNotePos.getLevel() + 1);
                notePos.setLft(refNotePos.getRgt());
                notePos.setRgt(refNotePos.getRgt() + 1);
                notePos.setParentId(refNoteId);

                /*
                 * If note is being created under already folded note, mark it as such
                 * so it doesn't show up.
                 */
                if (refNotePos.isFolded()) {
                    notePos.setFoldedUnderId(refNoteId);
                }

                break;

            case BELOW:
                notePos.setLevel(refNotePos.getLevel());
                notePos.setLft(refNotePos.getRgt() + 1);
                notePos.setRgt(refNotePos.getRgt() + 2);
                notePos.setParentId(refNotePos.getParentId());

                break;

            case UNDEFINED:
                /* If target note is not used, add note at the end with level 1. */
                long rootRgt = getMaxRgt(db, bookId);
                long rootId = getRootId(db, bookId);

                notePos.setLevel(1);
                notePos.setLft(rootRgt);
                notePos.setRgt(rootRgt + 1);
                notePos.setParentId(rootId);

                break;

            default:
                throw new IllegalArgumentException("Unsupported placement for new note: " + placement);
        }

        switch (placement) {
            case ABOVE:
            case UNDER:
            case BELOW:
                /* Make space for new note - increment notes' LFT and RGT. */
                DatabaseUtils.makeSpaceForNewNotes(db, 1, refNotePos, placement);

                /* Update number of descendants. */
                updateDescendantsCountOfAncestors(db, bookId, notePos.getLft(), notePos.getRgt());

            case UNDEFINED:
                /* Make space for new note - increment root's RGT. */
                String selection = DbNote.Column.BOOK_ID + " = " + bookId + " AND " + DbNote.Column.LFT + " = 1";
                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection, 2, ProviderContract.Notes.UpdateParam.RGT);
        }

        notePos.setBookId(bookId);

        DbNote.toContentValues(values, notePos);

        replaceTimestampRangeStringsWithIds(db, values);

        long id = db.insertOrThrow(DbNote.TABLE, null, values);

        return ContentUris.withAppendedId(uri, id);
    }

    private void updateDescendantsCountOfAncestors(SQLiteDatabase db, long bookId, long lft, long rgt) {
        db.execSQL("UPDATE " + DbNote.TABLE +
                   " SET " + ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT + " = " + ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT + " + 1 " +
                   "WHERE " + DatabaseUtils.whereAncestors(bookId, lft, rgt));
    }

    private int getMaxRgt(SQLiteDatabase db, long bookId) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] { "MAX(" + ProviderContract.Notes.QueryParam.RGT + ")" },
                ProviderContract.Notes.QueryParam.BOOK_ID + "= " + bookId + " AND " + ProviderContract.Notes.QueryParam.IS_CUT + " = 0",
                null,
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return 0;
            }

        } finally {
            cursor.close();
        }
    }

    private long getRootId(SQLiteDatabase db, long bookId) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                DatabaseUtils.PROJECTION_FOR_ID,
                ProviderContract.Notes.QueryParam.BOOK_ID + "= " + bookId + " AND " + ProviderContract.Notes.QueryParam.LEVEL + " = 0",
                null,
                null,
                null,
                null
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return 0;
            }

        } finally {
            cursor.close();
        }
    }

    private Uri bookSavedToRepo(SQLiteDatabase db, Uri uri, ContentValues values) {
        long bookId = Long.parseLong(uri.getPathSegments().get(1));
        String repoUrl = values.getAsString(ProviderContract.BooksIdSaved.Param.REPO_URL);
        String rookUrl = values.getAsString(ProviderContract.BooksIdSaved.Param.ROOK_URL);
        String rookRevision = values.getAsString(ProviderContract.BooksIdSaved.Param.ROOK_REVISION);
        long rookMtime = values.getAsLong(ProviderContract.BooksIdSaved.Param.ROOK_MTIME);

        long repoId = getOrInsertRepoUrl(db, repoUrl);
        long rookUrlId = DbRookUrl.getOrInsert(db, rookUrl);
        long rookId = getOrInsertRook(db, rookUrlId, repoId);
        long versionedRookId = getOrInsertVersionedRook(db, rookId, rookRevision, rookMtime);

        updateOrInsertBookLink(db, bookId, repoUrl, rookUrl);
        updateOrInsertBookSync(db, bookId, repoUrl, rookUrl, rookRevision, rookMtime);

        db.rawQuery(DELETE_CURRENT_VERSIONED_ROOKS_FOR_ROOK_ID, new String[] { String.valueOf(rookId) });

        ContentValues v = new ContentValues();
        v.put(DbCurrentVersionedRook.Column.VERSIONED_ROOK_ID, versionedRookId);
        db.insert(DbCurrentVersionedRook.TABLE, null, v);

        return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), bookId);
    }

    private static final String VERSIONED_ROOK_IDS_FOR_ROOK_ID =
            "SELECT _id FROM " + DbVersionedRook.TABLE + " WHERE " + DbVersionedRook.Column.ROOK_ID + "=?";

    private static final String DELETE_CURRENT_VERSIONED_ROOKS_FOR_ROOK_ID =
            "DELETE FROM " + DbCurrentVersionedRook.TABLE +
            " WHERE " + DbCurrentVersionedRook.Column.VERSIONED_ROOK_ID + " IN (" + VERSIONED_ROOK_IDS_FOR_ROOK_ID + ")";

    private Uri insertCurrentRook(SQLiteDatabase db, Uri uri, ContentValues contentValues) {
        String repoUrl = contentValues.getAsString(ProviderContract.CurrentRooks.Param.REPO_URL);
        String rookUrl = contentValues.getAsString(ProviderContract.CurrentRooks.Param.ROOK_URL);
        String revision = contentValues.getAsString(ProviderContract.CurrentRooks.Param.ROOK_REVISION);
        long mtime = contentValues.getAsLong(ProviderContract.CurrentRooks.Param.ROOK_MTIME);

        long repoUrlId = getOrInsertRepoUrl(db, repoUrl);
        long rookUrlId = DbRookUrl.getOrInsert(db, rookUrl);
        long rookId = getOrInsertRook(db, rookUrlId, repoUrlId);
        long versionedRookId = getOrInsertVersionedRook(db, rookId, revision, mtime);

        ContentValues values = new ContentValues();
        values.put(DbCurrentVersionedRook.Column.VERSIONED_ROOK_ID, versionedRookId);
        long id = db.insert(DbCurrentVersionedRook.TABLE, null, values);

        return ContentUris.withAppendedId(uri, id);
    }

    private long getOrInsertVersionedRook(SQLiteDatabase db, long rookId, String revision, long mtime) {
        long id = DatabaseUtils.getId(
                db,
                DbVersionedRook.TABLE,
                DbVersionedRook.Column.ROOK_ID + "=? AND " + DbVersionedRook.Column.ROOK_REVISION + "=? AND " + DbVersionedRook.Column.ROOK_MTIME + "=?",
                new String[] { String.valueOf(rookId), revision, String.valueOf(mtime) });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(DbVersionedRook.Column.ROOK_ID, rookId);
            values.put(DbVersionedRook.Column.ROOK_REVISION, revision);
            values.put(DbVersionedRook.Column.ROOK_MTIME, mtime);

            id = db.insertOrThrow(DbVersionedRook.TABLE, null, values);
        }

        return id;
    }

    private long getOrInsertRook(SQLiteDatabase db, long rookUrlId, long repoId) {
        long id = DatabaseUtils.getId(
                db,
                DbRook.TABLE,
                DbRook.Column.ROOK_URL_ID + "=? AND " + DbRook.Column.REPO_ID + "=?",
                new String[] { String.valueOf(rookUrlId), String.valueOf(repoId) });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(DbRook.Column.ROOK_URL_ID, rookUrlId);
            values.put(DbRook.Column.REPO_ID, repoId);

            id = db.insertOrThrow(DbRook.TABLE, null, values);
        }

        return id;
    }

    private long getOrInsertRepoUrl(SQLiteDatabase db, String repoUrl) {
        long id = DatabaseUtils.getId(
                db,
                DbRepo.TABLE,
                DbRepo.Column.REPO_URL + "=?",
                new String[] { repoUrl });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(DbRepo.Column.REPO_URL, repoUrl);
            values.put(DbRepo.Column.IS_REPO_ACTIVE, 0);

            id = db.insertOrThrow(DbRepo.TABLE, null, values);
        }

        return id;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), selection, selectionArgs);

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        Set<Uri> notifyUris = new HashSet<>();

        int result = delete(db, notifyUris, uri, selection, selectionArgs);

        for (Uri notifyUri: notifyUris) {
            notifyChange(getContext(), notifyUri);
        }

        return result;
    }

    private int delete(SQLiteDatabase db, Set<Uri> notifyUris, Uri uri, String selection, String[] selectionArgs) {
        int result;
        long noteId;
        String table;

        switch (uris.matcher.match(uri)) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                result = DbRepo.delete(db, notifyUris, selection, selectionArgs);
                return result;

            /* Delete repo by just marking it as such. */
            case ProviderUris.REPOS_ID:
                selection = DbRepo.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;

                /* Remove books' links which are using this repo. */
                removeBooksLinksForRepo(db, uri.getLastPathSegment());

                /* Delete repo itself. */
                result = DbRepo.delete(db, notifyUris, selection, selectionArgs);

                return result;

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                break;

            case ProviderUris.FILTERS_ID:
                table = DbSearch.TABLE;
                selection = DbSearch.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.BOOKS:
                table = DbBook.TABLE;
                break;

            case ProviderUris.BOOKS_ID:
                table = DbBook.TABLE;
                selection = DbBook.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.NOTES:
                table = DbNote.TABLE;
                break;

            case ProviderUris.CURRENT_ROOKS:
                table = DbCurrentVersionedRook.TABLE;
                break;

            case ProviderUris.LINKS_FOR_BOOK:
                table = DbBookLink.TABLE;
                selection = DbBookLink.Column.BOOK_ID + " = " + Long.parseLong(uri.getPathSegments().get(1));
                selectionArgs = null;
                result = db.delete(table, selection, selectionArgs);
                notifyUris.add(ProviderContract.Books.ContentUri.books());
                return result;

            case ProviderUris.NOTES_ID_PROPERTIES:
                noteId = Long.parseLong(uri.getPathSegments().get(1));

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.getQuery());

                table = DbNoteProperty.TABLE;

                selection = DbNoteProperty.Column.NOTE_ID + " = " + noteId;
                selectionArgs = null;

                break;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        result = db.delete(table, selection, selectionArgs);

        notifyUris.add(uri);

        return result;
    }

    private int removeBooksLinksForRepo(SQLiteDatabase db, String repoId) {
        /* Rooks which use passed repo. */
        String rookIds = "SELECT DISTINCT " + DbRook.Column._ID +
                         " FROM " + DbRook.TABLE +
                         " WHERE " + DbRook.Column.REPO_ID + " = " + repoId;

        return db.delete(DbBookLink.TABLE, DbBookLink.Column.ROOK_ID + " IN (" + rookIds + ")", null);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), contentValues, selection, selectionArgs);

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        Set<Uri> notifyUris = new HashSet<>();

        int result = updateUnderTransaction(db, notifyUris, uri, contentValues, selection, selectionArgs);

        for (Uri notifyUri: notifyUris) {
            notifyChange(getContext(), notifyUri);
        }

        return result;
    }

    // TODO: Make sure everything is done under transaction. Careful not to close db
    private int updateUnderTransaction(SQLiteDatabase db, Set<Uri> notifyUris, Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int result;
        long noteId;
        int match = uris.matcher.match(uri);

        String table;

        switch (match) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                result = DbRepo.update(db, notifyUris, contentValues, selection, selectionArgs);
                return result;

            case ProviderUris.REPOS_ID:
                selection = DbRepo.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                result = DbRepo.update(db, notifyUris, contentValues, selection, selectionArgs);
                return result;

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                break;

            case ProviderUris.FILTERS_ID:
                table = DbSearch.TABLE;
                selection = DbSearch.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.FILTER_UP:
                result = ProviderFilters.moveFilterUp(db, Long.parseLong(uri.getPathSegments().get(1)));
                notifyUris.add(ProviderContract.Filters.ContentUri.filters());
                return result;

            case ProviderUris.FILTER_DOWN:
                result = ProviderFilters.moveFilterDown(db, Long.parseLong(uri.getPathSegments().get(1)));
                notifyUris.add(ProviderContract.Filters.ContentUri.filters());
                return result;

            case ProviderUris.BOOKS:
                table = DbBook.TABLE;
                break;

            case ProviderUris.BOOKS_ID:
                table = DbBook.TABLE;
                selection = DbBook.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.NOTES:
                table = DbNote.TABLE;
                replaceTimestampRangeStringsWithIds(db, contentValues);
                break;

            case ProviderUris.NOTE:
                selection = DbNote.Column._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;

                replaceTimestampRangeStringsWithIds(db, contentValues);

                result = db.update(DbNote.TABLE, contentValues, selection, selectionArgs);

                // TODO: Ugh: Use /books/1/notes/23/ or just move to constant
                if (uri.getQueryParameter("book-id") != null) {
                    DatabaseUtils.updateBookMtime(db, Long.parseLong(uri.getQueryParameter("book-id")));
                }

                notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                notifyUris.add(ProviderContract.Books.ContentUri.books());

                return result;

            case ProviderUris.NOTES_STATE:
                String ids = contentValues.getAsString(ProviderContract.NotesState.Param.NOTE_IDS);
                String state = contentValues.getAsString(ProviderContract.NotesState.Param.STATE);

                result = setStateForNotes(db, ids, state);

                if (result > 0) {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

                return result;

            case ProviderUris.CUT:
                try {
                    Action action = new CutNotesAction(contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

            case ProviderUris.PASTE:
                try {
                    Action action = new PasteNotesAction(contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

            case ProviderUris.DELETE:
                try {
                    Action action = new DeleteNotesAction(contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

            case ProviderUris.PROMOTE:
                try {
                    Action action = new PromoteNotesAction(contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

            case ProviderUris.DEMOTE:
                try {
                    Action action = new DemoteNotesAction(contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

            case ProviderUris.MOVE:
                try {
                    Action action = new MoveNotesAction(contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                    notifyUris.add(ProviderContract.Books.ContentUri.books());
                }

            case ProviderUris.NOTE_TOGGLE_FOLDED_STATE:
                try {
                    noteId = Long.valueOf(uri.getPathSegments().get(1));

                    Action action = new ToggleFoldedStateAction(noteId);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                }

            case ProviderUris.BOOKS_ID_CYCLE_VISIBILITY:
                try {
                    long bookId = Long.valueOf(uri.getPathSegments().get(1));

                    Action action = new CycleVisibilityAction(bookId);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                }

            case ProviderUris.BOOKS_ID_SPARSE_TREE:
                try {
                    long bookId = Long.valueOf(uri.getPathSegments().get(1));

                    Action action = new SparseTreeAction(bookId, contentValues);
                    return ActionRunner.run(db, action);

                } finally {
                    notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                }

            case ProviderUris.LINKS_FOR_BOOK:
                result = updateLinkForBook(db, uri, contentValues);
                notifyUris.add(ProviderContract.Books.ContentUri.books());
                return result;

            case ProviderUris.DB_RECREATE:
                mOpenHelper.reCreateTables(db);

                // TODO: Forgetting to update this code after new table is added - move to Database at least
                notifyUris.add(ProviderContract.Books.ContentUri.books());
                notifyUris.add(ProviderContract.Notes.ContentUri.notes());
                notifyUris.add(ProviderContract.Repos.ContentUri.repos());
                notifyUris.add(ProviderContract.Filters.ContentUri.filters());
                notifyUris.add(ProviderContract.LocalDbRepo.ContentUri.dbRepos());

                return 0;

            case ProviderUris.DB_SWITCH:
                Context testContext = new RenamingDelegatingContext(getContext(), "test_");

                mOpenHelper.close();
                mOpenHelper = new Database(testContext);

                return 0;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        // FIXME: Hard to read - some cases above return, some are reaching this
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, table, contentValues, selection, selectionArgs);
        result = db.update(table, contentValues, selection, selectionArgs);

        notifyUris.add(uri);

        return result;
    }


    /**
     * @param ids Note ids separated with comma
     * @param targetState keyword
     * @return Number of notes that have been updated
     */
    private int setStateForNotes(SQLiteDatabase db, String ids, String targetState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db, ids, targetState);

        int notesUpdated;

        db.beginTransaction();
        try {
            /* Select only notes which don't already have the target state. */
            String notesSelection = DbNote.Column._ID + " IN (" + ids + ") AND (" +
                                    NotesView.Columns.STATE + " IS NULL OR " + NotesView.Columns.STATE + " != ?)";

            String[] selectionArgs = new String[] { targetState };

            /* Select notebooks which will be affected. */
            String booksSelection = DbBook.Column._ID + " IN (SELECT DISTINCT " +
                                    DbNote.Column.BOOK_ID + " FROM " + DbNote.TABLE + " WHERE " + notesSelection + ")";

            /* Notebooks must be updated before notes, because selection checks
             * for notes what will be affected.
             */
            DatabaseUtils.updateBookMtime(db, booksSelection, selectionArgs);

            /* Update notes. */
            if (AppPreferences.isDoneKeyword(getContext(), targetState)) {
                notesUpdated = setDoneStateForNotes(db, targetState, notesSelection, selectionArgs);
            } else {
                notesUpdated = setOtherStateForNotes(db, targetState, notesSelection, selectionArgs);
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }

        return notesUpdated;
    }

    /**
     * Removes CLOSED timestamp and simply sets the state.
     */
    private int setOtherStateForNotes(SQLiteDatabase db, String targetState, String notesSelection, String[] selectionArgs) {
        ContentValues values = new ContentValues();
        values.put(DbNote.Column.STATE, targetState);
        values.putNull(DbNote.Column.CLOSED_RANGE_ID);

        return db.update(DbNote.TABLE, values, notesSelection, selectionArgs);
    }

    /**
     * If original state is to-do and repeater exists
     * keep the state intact and shift timestamp.
     *
     * If current state is to-do and there is no repeater
     * set the state and keep the timestamp intact.
     */
    private int setDoneStateForNotes(SQLiteDatabase db, String state, String notesSelection, String[] selectionArgs) {
        int notesUpdated = 0;

        /* Get all notes that don't already have the same state. */
        Cursor cursor = db.query(
                NotesView.VIEW_NAME,
                new String[] {
                        NotesView.Columns._ID,
                        NotesView.Columns.SCHEDULED_RANGE_STRING,
                        NotesView.Columns.DEADLINE_RANGE_STRING,
                        NotesView.Columns.STATE,
                },
                notesSelection,
                selectionArgs,
                null,
                null,
                null);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found notes: " + cursor.getCount());

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                StateChangeLogic stateSetOp = new StateChangeLogic(
                        AppPreferences.todoKeywordsSet(getContext()),
                        AppPreferences.doneKeywordsSet(getContext())
                );

                stateSetOp.setState(state,
                        cursor.getString(3),
                        OrgRange.getInstanceOrNull(cursor.getString(1)),
                        OrgRange.getInstanceOrNull(cursor.getString(2)));

                ContentValues values = new ContentValues();

                if (stateSetOp.getState() != null) {
                    values.put(ProviderContract.Notes.UpdateParam.STATE, stateSetOp.getState());
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.STATE);
                }

                if (stateSetOp.getScheduled() != null) {
                    values.put(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING, stateSetOp.getScheduled().toString());
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
                }

                if (stateSetOp.getDeadline() != null) {
                    values.put(ProviderContract.Notes.UpdateParam.DEADLINE_STRING, stateSetOp.getDeadline().toString());
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
                }

                if (stateSetOp.getClosed() != null) {
                    values.put(ProviderContract.Notes.UpdateParam.CLOSED_STRING, stateSetOp.getClosed().toString());
                } else {
                    values.putNull(ProviderContract.Notes.UpdateParam.CLOSED_STRING);
                }

                replaceTimestampRangeStringsWithIds(db, values);

                notesUpdated += db.update(DbNote.TABLE, values, DbNote.Column._ID + "=" + cursor.getString(0), null);
            }

        } finally {
            cursor.close();
        }

        return notesUpdated;
    }

    private int updateLinkForBook(SQLiteDatabase db, Uri uri, ContentValues contentValues) {
        long bookId = Long.parseLong(uri.getPathSegments().get(1));
        String repoUrl = contentValues.getAsString(ProviderContract.BookLinks.Param.REPO_URL);
        String rookUrl = contentValues.getAsString(ProviderContract.BookLinks.Param.ROOK_URL);

        if (repoUrl == null || rookUrl == null) {
            /* Remove link for book. */
            db.delete(DbBookLink.TABLE, DbBookLink.Column.BOOK_ID + "=" + bookId, null);
        } else {
            updateOrInsertBookLink(db, bookId, repoUrl, rookUrl);
        }

        return 1;
    }

    private int updateOrInsertBookLink(SQLiteDatabase db, long bookId, String repoUrl, String rookUrl) {
        long repoUrlId = getOrInsertRepoUrl(db, repoUrl);
        long rookUrlId = DbRookUrl.getOrInsert(db, rookUrl);
        long rookId = getOrInsertRook(db, rookUrlId, repoUrlId);

        ContentValues values = new ContentValues();
        values.put(DbBookLink.Column.BOOK_ID, bookId);
        values.put(DbBookLink.Column.ROOK_ID, rookId);

        long id = DatabaseUtils.getId(db, DbBookLink.TABLE, DbBookLink.Column.BOOK_ID + "=" + bookId, null);
        if (id != 0) {
            db.update(DbBookLink.TABLE, values, DbBookLink.Column._ID + "=" + id, null);
        } else {
            db.insertOrThrow(DbBookLink.TABLE, null, values);
        }

        return 1;
    }

    private int updateOrInsertBookSync(
            SQLiteDatabase db,
            long bookId,
            String repoUrl,
            String rookUrl,
            String revision,
            long mtime) {

        long repoUrlId = getOrInsertRepoUrl(db, repoUrl);
        long rookUrlId = DbRookUrl.getOrInsert(db, rookUrl);
        long rookId = getOrInsertRook(db, rookUrlId, repoUrlId);
        long rookRevisionId = getOrInsertVersionedRook(db, rookId, revision, mtime);

        ContentValues values = new ContentValues();
        values.put(DbBookSync.Column.BOOK_VERSIONED_ROOK_ID, rookRevisionId);
        values.put(DbBookSync.Column.BOOK_ID, bookId);

        long id = DatabaseUtils.getId(db, DbBookSync.TABLE, DbBookSync.Column.BOOK_ID + "=" + bookId, null);
        if (id != 0) {
            db.update(DbBookSync.TABLE, values, DbBookSync.Column._ID + "=" + id, null);
        } else {
            db.insertOrThrow(DbBookSync.TABLE, null, values);
        }

        return 1;
    }


    private Uri getOrInsertBook(SQLiteDatabase db, String name) {
        Cursor cursor = db.query(
                DbBook.TABLE,
                new String[] { DbBook.Column._ID },
                DbBook.Column.NAME + "=?",
                new String[] { name },
                null,
                null,
                null);

        try {
            if (cursor.moveToFirst()) {
                return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), cursor.getLong(0));
            }
        } finally {
            cursor.close();
        }

        /* No such book, create it. */

        ContentValues values = new ContentValues();
        values.put(DbBook.Column.NAME, name);

        long id = db.insertOrThrow(DbBook.TABLE, null, values);

        return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), id);
    }

    private Uri loadBookFromFile(ContentValues values) {
        String bookName = values.getAsString(ProviderContract.LoadBookFromFile.Param.BOOK_NAME);
        String format = values.getAsString(ProviderContract.LoadBookFromFile.Param.FORMAT);
        String filePath = values.getAsString(ProviderContract.LoadBookFromFile.Param.FILE_PATH);
        String repoUrl = values.getAsString(ProviderContract.LoadBookFromFile.Param.ROOK_REPO_URL);
        String rookUrl = values.getAsString(ProviderContract.LoadBookFromFile.Param.ROOK_URL);
        String rookRevision = values.getAsString(ProviderContract.LoadBookFromFile.Param.ROOK_REVISION);
        long rookMtime = values.containsKey(ProviderContract.LoadBookFromFile.Param.ROOK_MTIME) ? values.getAsLong(ProviderContract.LoadBookFromFile.Param.ROOK_MTIME) : 0;
        String selectedEncoding = values.getAsString(ProviderContract.LoadBookFromFile.Param.SELECTED_ENCODING);

        try {
            /*
             * Determine encoding to use -- detect of force it.
             */
            String usedEncoding;
            String detectedEncoding = null;

            if (selectedEncoding == null) {
                usedEncoding = detectedEncoding = EncodingDetect.getInstance(new FileInputStream(new File(filePath))).getEncoding();
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Detected encoding: " + detectedEncoding);

                /* Can't detect encoding - use default. */
                if (detectedEncoding == null) {
                    usedEncoding = "UTF-8";
                    Log.w(TAG, "Encoding for " + bookName + " could not be detected, using UTF-8");
                }

            } else {
                usedEncoding = selectedEncoding;
                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Using selected encoding: " + usedEncoding);
            }

            return loadBookFromReader(
                    bookName,
                    repoUrl,
                    rookUrl,
                    rookRevision,
                    rookMtime,
                    format,
                    new InputStreamReader(new FileInputStream(new File(filePath)), usedEncoding),
                    usedEncoding,
                    detectedEncoding,
                    selectedEncoding
            );

        } catch (IOException e) {
            e.printStackTrace();

            /* Remember that the Android system must be able to communicate the Exception
             * across process boundaries. This is one of those.
             */
            throw new IllegalArgumentException(e);
        }
    }

    private Uri loadBookFromReader(
            final String bookName,
            final String repoUrl,
            final String rookUrl,
            final String rookRevision,
            final long rookMtime,
            final String format,
            final Reader inReader,
            final String usedEncoding,
            final String detectedEncoding,
            final String selectedEncoding) throws IOException {

        long startedAt = System.currentTimeMillis();

        Uri uri;

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        /* Create book if it doesn't already exist. */
        uri = getOrInsertBook(db, bookName);

        final long bookId = ContentUris.parseId(uri);

        /* Delete all notes from book. TODO: Delete all other references to this book ID */
        db.delete(DbNote.TABLE, DbNote.Column.BOOK_ID + "=" + bookId, null);

        /* Open reader. */
        Reader reader = new BufferedReader(inReader);
        try {
            /*
             * Create and run parser.
             * When multiple formats are supported, decide which parser to use here.
             */
            new OrgParser.Builder()
                    .setInput(reader)
                    .setTodoKeywords(AppPreferences.todoKeywordsSet(getContext()))
                    .setDoneKeywords(AppPreferences.doneKeywordsSet(getContext()))
                    .setListener(new OrgNestedSetParserListener() {
                        @Override
                        public void onNode(OrgNodeInSet node) throws IOException {
                            BookSizeValidator.validate(node);

                            /* Insert note with book id at specific position. */

                            NotePosition position = new NotePosition();
                            position.setBookId(bookId);
                            position.setLft(node.getLft());
                            position.setRgt(node.getRgt());
                            position.setLevel(node.getLevel());
                            position.setDescendantsCount(node.getDescendantsCount());
                            position.setFoldedUnderId(0);

                            ContentValues values = new ContentValues();

                            DbNote.toContentValues(values, position);
                            DbNote.toContentValues(db, values, node.getHead());
                            // NotesClient.toContentValues(values, node.getHead());

                            long noteId = db.insertOrThrow(DbNote.TABLE, null, values);

                            // replaceTimestampRangeStringsWithIds(db, values);
                            // replacePropertiesWithIds(db, noteId, values);

                            /* Insert properties for newly created note. */
                            int i = 0;
                            for (OrgProperty property: node.getHead().getProperties()) {
                                new DbNoteProperty(
                                        noteId,
                                        i++,
                                        new DbProperty(
                                                new DbPropertyName(property.getName()),
                                                new DbPropertyValue(property.getValue()))
                                ).save(db);
                            }
                        }

                        @Override
                        public void onFile(OrgFile file) throws IOException {
                            BookSizeValidator.validate(file);

                            ContentValues values = new ContentValues();

                            BooksClient.toContentValues(values, file.getSettings());

                                /* Set preface. TODO: Move to and rename OrgFileSettings */
                            values.put(DbBook.Column.PREFACE, file.getPreface());

                            values.put(DbBook.Column.USED_ENCODING, usedEncoding);
                            values.put(DbBook.Column.DETECTED_ENCODING, detectedEncoding);
                            values.put(DbBook.Column.SELECTED_ENCODING, selectedEncoding);

                            db.update(DbBook.TABLE, values, DbBook.Column._ID + "=" + bookId, null);
                        }

                    })
                    .build()
                    .parse();

        } finally {
            reader.close();
        }

        if (rookUrl != null) {
            updateOrInsertBookLink(db, bookId, repoUrl, rookUrl);
            updateOrInsertBookSync(db, bookId, repoUrl, rookUrl, rookRevision, rookMtime);
        }

        DatabaseUtils.updateParentIds(db, bookId);

        /* Update book with modification time and mark it as complete. */
        ContentValues values = new ContentValues();
        values.put(DbBook.Column.IS_DUMMY, 0);

        db.update(DbBook.TABLE, values, DbBook.Column._ID + "=" + bookId, null);


        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, bookName + ": " + (System.currentTimeMillis() - startedAt) + "ms");

        return uri;
    }

    private void replaceTimestampRangeStringsWithIds(SQLiteDatabase db, ContentValues values) {

        if (values.containsKey(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.Column.SCHEDULED_RANGE_ID, getOrInsertOrgRange(db, OrgRange.getInstance(str)));
            } else {
                values.putNull(DbNote.Column.SCHEDULED_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.DEADLINE_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.Column.DEADLINE_RANGE_ID, getOrInsertOrgRange(db, OrgRange.getInstance(str)));
            } else {
                values.putNull(DbNote.Column.DEADLINE_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.CLOSED_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.CLOSED_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.Column.CLOSED_RANGE_ID, getOrInsertOrgRange(db, OrgRange.getInstance(str)));
            } else {
                values.putNull(DbNote.Column.CLOSED_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.CLOSED_STRING);
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.CLOCK_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.CLOCK_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.Column.CLOCK_RANGE_ID, getOrInsertOrgRange(db, OrgRange.getInstance(str)));
            } else {
                values.putNull(DbNote.Column.CLOCK_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.CLOCK_STRING);
        }
    }

    /**
     * Gets {@link OrgDateTime} from database or inserts a new record if it doesn't exist.
     * @return {@link OrgDateTime} database ID
     */
    private long getOrInsertOrgRange(SQLiteDatabase db, OrgRange range) {
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

    private long getOrInsertOrgTime(SQLiteDatabase db, OrgDateTime orgDateTime) {
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

    /**
     * Notify observers (such as {@link android.widget.CursorAdapter}) that the data changed.
     */
    private static void notifyChange(Context context, Uri uri) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString());
        context.getContentResolver().notifyChange(uri, null);
    }


    /**
     * Don't allow books which have huge fields.  Storing is fine, but Cursor has limits:
     * Couldn't read row 0, col 0 from CursorWindow.  Make sure the Cursor is initialized correctly before accessing data from it.
     * See config_cursorWindowSize.
     *
     * TODO: Also validate before every write to database, not just when parsing fields.
     * User can paste huge content to a note directly.
     */
    public static class BookSizeValidator {
        // TODO: This now works with lion-wide.org (test asset) - how to decide which size to use?
        private static final int BOOK_PREFACE_LIMIT = 1000000;
        private static final int NOTE_TOTAL_SIZE_LIMIT = 1500000;

        public static void validate(OrgFile agenda) throws IOException {
            if (agenda.getPreface().length() > BOOK_PREFACE_LIMIT) {
                throw new IOException("Notebook content is too big (" + agenda.getPreface().length() + " chars " + BOOK_PREFACE_LIMIT + " max)");
            }
        }

        public static void validate(OrgNode node) throws IOException {
            /*
             * Assume indent (more space occupied).
             * This probably slows down the import a lot.
             */
            OrgParserWriter parserWriter = new OrgParserWriter();
            int totalSize = parserWriter.whiteSpacedHead(node, true).length();

            if (totalSize > NOTE_TOTAL_SIZE_LIMIT) {
                throw new IOException("Note total size is too big (" + totalSize + " chars " + NOTE_TOTAL_SIZE_LIMIT + " max)");
            }
        }
    }

}
