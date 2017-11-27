package com.orgzly.android.provider;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.orgzly.BuildConfig;
import com.orgzly.android.Note;
import com.orgzly.android.NotePosition;
import com.orgzly.android.SearchQuery;
import com.orgzly.android.provider.views.DbTimeView;
import com.orgzly.org.utils.StateChangeLogic;
import com.orgzly.android.prefs.AppPreferences;
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
import com.orgzly.android.provider.models.DbNoteAncestor;
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
import com.orgzly.android.provider.views.DbBookView;
import com.orgzly.android.provider.views.DbNoteView;
import com.orgzly.android.ui.Place;
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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.orgzly.android.provider.GenericDatabaseUtils.field;
import static com.orgzly.android.provider.GenericDatabaseUtils.join;

public class Provider extends ContentProvider {
    private static final String TAG = Provider.class.getName();

    public static String DATABASE_NAME = "orgzly.db";
    public static String DATABASE_NAME_FOR_TESTS = "orgzly_test.db";

    protected Database mOpenHelper;

    private final ProviderUris uris = new ProviderUris();

    private final ThreadLocal<Boolean> inBatch = new ThreadLocal<>();

    private boolean isInBatch() {
        return inBatch.get() != null && inBatch.get();
    }

    @Override
    public boolean onCreate() {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG);

        /*
         * Creates a new helper object. This method always returns quickly.
         * Notice that the database itself isn't created or opened
         * until SQLiteOpenHelper.getWritableDatabase is called
         */
        mOpenHelper = new Database(getContext(), DATABASE_NAME);

        return true;
    }

    @Override
    public String getType(Uri uri) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString());
        return null;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        ContentProviderResult[] results;

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            inBatch.set(true);
            results = super.applyBatch(operations);
            inBatch.set(false);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyChange();

        return results;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), projection, selection, selectionArgs, sortOrder);

        /* Gets a readable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        String table;
        Cursor cursor = null;

        switch (uris.matcher.match(uri)) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                table = DbRepo.TABLE;
                selection = DbRepo.IS_REPO_ACTIVE + "= 1";
                selectionArgs = null;
                break;

            case ProviderUris.REPOS_ID:
                table = DbRepo.TABLE;
                selection = DbRepo._ID + "=?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;

            case ProviderUris.NOTES:
                table = DbNoteView.VIEW_NAME;
                break;

            case ProviderUris.NOTES_SEARCH_QUERIED:
                table = null;
                cursor = queryNotesSearchQueried(db, uri.getQuery(), sortOrder);
                break;

            case ProviderUris.BOOKS_ID_NOTES:
                table = DbNoteView.VIEW_NAME;

                long bookId = Long.parseLong(uri.getPathSegments().get(1));

                selection = DbNoteView.BOOK_ID + "=" + bookId + " AND " + DatabaseUtils.WHERE_VISIBLE_NOTES;
                selectionArgs = null;
                break;

            case ProviderUris.NOTES_WITH_PROPERTY:
                String propName = uri.getQueryParameter(ProviderContract.Notes.Param.PROPERTY_NAME);
                String propValue = uri.getQueryParameter(ProviderContract.Notes.Param.PROPERTY_VALUE);

                selection =
                        field("tpropertyname", DbPropertyName.NAME) + " = ? AND " +
                                field("tpropertyvalue", DbPropertyValue.VALUE) + " = ? AND " +
                                field("tnotes", DbNote._ID) + " IS NOT NULL";

                selectionArgs = new String[] { propName, propValue };

                sortOrder = field("tnotes", DbNote.LFT);

                table = DbNoteProperty.TABLE + " " +
                        join(DbNote.TABLE, "tnotes", DbNote._ID, DbNoteProperty.TABLE, DbNoteProperty.NOTE_ID) +
                        join(DbProperty.TABLE, "tproperties", DbProperty._ID, DbNoteProperty.TABLE, DbNoteProperty.PROPERTY_ID) +
                        join(DbPropertyName.TABLE, "tpropertyname", DbPropertyName._ID, "tproperties", DbProperty.NAME_ID) +
                        join(DbPropertyValue.TABLE, "tpropertyvalue", DbPropertyValue._ID, "tproperties", DbProperty.VALUE_ID);

                projection = new String[] {
                        field("tnotes", DbNote._ID),
                        field("tnotes", DbNote.BOOK_ID),
                };

                break;

            case ProviderUris.NOTES_ID_PROPERTIES:
                long noteId = Long.parseLong(uri.getPathSegments().get(1));

                selection = field(DbNoteProperty.TABLE, DbNoteProperty.NOTE_ID) + "=" + noteId;
                selectionArgs = null;

                sortOrder = DbNoteProperty.POSITION;

                table = DbNoteProperty.TABLE + " " +
                        join(DbProperty.TABLE, "tproperties", DbProperty._ID, DbNoteProperty.TABLE, DbNoteProperty.PROPERTY_ID) +
                        join(DbPropertyName.TABLE, "tpropertyname", DbPropertyName._ID, "tproperties", DbProperty.NAME_ID) +
                        join(DbPropertyValue.TABLE, "tpropertyvalue", DbPropertyValue._ID, "tproperties", DbProperty.VALUE_ID);

                projection = new String[] {
                        "tpropertyname." + DbPropertyName.NAME,
                        "tpropertyvalue." + DbPropertyValue.VALUE,
                };

                break;

            case ProviderUris.BOOKS:
                table = DbBookView.VIEW_NAME;
                break;

            case ProviderUris.BOOKS_ID:
                table = DbBookView.VIEW_NAME;
                selection = DbBook._ID + "=?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                break;

            case ProviderUris.FILTERS_ID:
                table = DbSearch.TABLE;
                selection = DbSearch._ID + "=?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
                break;

            case ProviderUris.CURRENT_ROOKS:
                projection = new String[] {
                        field(DbRepo.TABLE, DbRepo.REPO_URL),
                        field(DbRookUrl.TABLE, DbRookUrl.ROOK_URL),
                        field(DbVersionedRook.TABLE, DbVersionedRook.ROOK_REVISION),
                        field(DbVersionedRook.TABLE, DbVersionedRook.ROOK_MTIME),
                };

                table = DbCurrentVersionedRook.TABLE +
                        " LEFT JOIN " + DbVersionedRook.TABLE + " ON (" + field(DbVersionedRook.TABLE, DbVersionedRook._ID) + "=" + field(DbCurrentVersionedRook.TABLE, DbCurrentVersionedRook.VERSIONED_ROOK_ID) + ")" +
                        " LEFT JOIN " + DbRook.TABLE + " ON (" + field(DbRook.TABLE, DbRook._ID) + "=" + field(DbVersionedRook.TABLE, DbVersionedRook.ROOK_ID) + ")" +
                        " LEFT JOIN " + DbRookUrl.TABLE + " ON (" + field(DbRookUrl.TABLE, DbRookUrl._ID) + "=" + field(DbRook.TABLE, DbRook.ROOK_URL_ID) + ")" +
                        " LEFT JOIN " + DbRepo.TABLE + " ON (" + field(DbRepo.TABLE, DbRepo._ID) + "=" + field(DbRook.TABLE, DbRook.REPO_ID) + ")" +
                        "";
                break;

            case ProviderUris.TIMES:
                table = null;

                String query = "SELECT " +
                        DbTimeView.NOTE_ID + ", " +
                        DbTimeView.BOOK_ID + ", " +
                        DbTimeView.BOOK_NAME + ", " +
                        DbTimeView.NOTE_STATE + ", " +
                        DbTimeView.NOTE_TITLE + ", " +
                        DbTimeView.TIME_TYPE + ", " +
                        DbTimeView.ORG_TIMESTAMP_STRING +
                        " FROM " + DbTimeView.VIEW_NAME;

                cursor = db.rawQuery(query, null);

                break;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        if (cursor == null) {
            cursor = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Cursor count: " + cursor.getCount() + " for " +
                table + " " + selection + " " + (selectionArgs != null ? TextUtils.join(",", selectionArgs) : ""));

        cursor.setNotificationUri(getContext().getContentResolver(), ProviderContract.AUTHORITY_URI);

        return cursor;
    }

    /**
     * Builds query parameters from {@link SearchQuery}.
     */
    private Cursor queryNotesSearchQueried(SQLiteDatabase db, String query, String sortOrder) {
        SearchQuery searchQuery = new SearchQuery(query);

        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();

        // Loop through the various search criteria groups. The criteria inside
        // each group is AND-ed together. The groups themselves are OR-ed
        // together.
        for (SearchQuery.SearchQueryGroup group : searchQuery.groups) {

            if (selection.length() == 0) {
                selection.append(" ((");
            } else {
                selection.append(") OR (");
            }

            /* Skip cut notes. */
            selection.append(DatabaseUtils.WHERE_EXISTING_NOTES);

            /*
             * We are only searching for a tag within a string of tags.
             * "tag" will be found in "few tagy ones"
             */
            for (String tag: group.getTags()) {
                selection.append(" AND (")
                        .append(DbNoteView.TAGS).append(" LIKE ? OR ")
                        .append(DbNoteView.INHERITED_TAGS).append(" LIKE ?)");

                selectionArgs.add("%" + tag + "%");
                selectionArgs.add("%" + tag + "%");
            }

            for (String tag: group.getNotTags()) {
                selection.append(" AND (")
                        .append("COALESCE(").append(DbNoteView.TAGS).append(", '')").append(" NOT LIKE ? AND ")
                        .append("COALESCE(").append(DbNoteView.INHERITED_TAGS).append(", '')").append(" NOT LIKE ?)");

                selectionArgs.add("%" + tag + "%");
                selectionArgs.add("%" + tag + "%");
            }

            if (group.hasBookName()) {
                selection.append(" AND ").append(DbNoteView.BOOK_NAME).append(" = ?");
                selectionArgs.add(group.getBookName());
            }

            if (group.hasNotBookName()) {
                for (String name: group.getNotBookName()) {
                    selection.append(" AND ").append(DbNoteView.BOOK_NAME).append(" != ?");
                    selectionArgs.add(name);
                }
            }

            if (group.hasStateType()) {
                if ("todo".equalsIgnoreCase(group.getStateType())) {
                    searchQueryStates(selection, selectionArgs, "IN", AppPreferences.todoKeywordsSet(getContext()));

                } else if ("done".equalsIgnoreCase(group.getStateType())) {
                    searchQueryStates(selection, selectionArgs, "IN", AppPreferences.doneKeywordsSet(getContext()));

                } else if ("none".equalsIgnoreCase(group.getStateType())) {
                    selection.append(" AND COALESCE(" + DbNoteView.STATE + ", '') = ''");
                }
            }

            if (group.hasNotStateType()) {
                if ("todo".equalsIgnoreCase(group.getNotStateType())) {
                    searchQueryStates(selection, selectionArgs, "NOT IN", AppPreferences.todoKeywordsSet(getContext()));

                } else if ("done".equalsIgnoreCase(group.getNotStateType())) {
                    searchQueryStates(selection, selectionArgs, "NOT IN", AppPreferences.doneKeywordsSet(getContext()));

                } else if ("none".equalsIgnoreCase(group.getNotStateType())) {
                    selection.append(" AND COALESCE(" + DbNoteView.STATE + ", '') != ''");
                }
            }

            if (group.hasState()) {
                selection.append(" AND COALESCE(" + DbNoteView.STATE + ", '') = ?");
                selectionArgs.add(group.getState());
            }

            if (group.hasNotState()) {
                for (String state: group.getNotState()) {
                    selection.append(" AND COALESCE(" + DbNoteView.STATE + ", '') != ?");
                    selectionArgs.add(state);
                }
            }

            for (String token: group.getTextSearch()) {
                selection.append(" AND (").append(DbNoteView.TITLE).append(" LIKE ?");
                selectionArgs.add("%" + token + "%");
                selection.append(" OR ").append(DbNoteView.CONTENT).append(" LIKE ?");
                selectionArgs.add("%" + token + "%");
                selection.append(" OR ").append(DbNoteView.TAGS).append(" LIKE ?");
                selectionArgs.add("%" + token + "%");
                selection.append(")");
            }

            if (group.hasScheduled()) {
                appendBeforeInterval(selection, DbNoteView.SCHEDULED_TIME_TIMESTAMP, group.getScheduled());
            }

            if (group.hasDeadline()) {
                appendBeforeInterval(selection, DbNoteView.DEADLINE_TIME_TIMESTAMP, group.getDeadline());
            }

            /*
             * Handle empty string and NULL - use default priority in those cases.
             */
            if (group.hasPriority()) {
                String defaultPriority = AppPreferences.defaultPriority(getContext());
                selection.append(" AND lower(coalesce(nullif(" + DbNoteView.PRIORITY + ", ''), ?)) = ?");
                selectionArgs.add(defaultPriority);
                selectionArgs.add(group.getPriority());
            }
            if (group.hasNotPriority()) {
                String defaultPriority = AppPreferences.defaultPriority(getContext());
                selection.append(" AND lower(coalesce(nullif(" + DbNoteView.PRIORITY + ", ''), ?)) != ?");
                selectionArgs.add(defaultPriority);
                selectionArgs.add(group.getNotPriority());
            }

            /*
             * Just check for note's *set* priority, don't use default if there is none.
             */
            if (group.hasSetPriority()) {
                selection.append(" AND lower(coalesce(" + DbNoteView.PRIORITY + ", '')) = ?");
                selectionArgs.add(group.getSetPriority());
            }

            if (group.hasNotSetPriority()) {
                selection.append(" AND lower(coalesce(" + DbNoteView.PRIORITY + ", '')) != ?");
                selectionArgs.add(group.getNotSetPriority());
            }

            if (group.hasNoteTags()) {
                /*
                 * We are only searching for a tag within a string of tags.
                 * "tag" will be found in "few tagy ones"
                 * Tags must be kept separately so we can match them exactly.
                 */
                for (String tag: group.getNoteTags()) {
                    selection.append(" AND ").append(DbNoteView.TAGS).append(" LIKE ?");
                    selectionArgs.add("%" + tag + "%");
                }
            }
        }

        selection.append(")) ");

        String sql = "SELECT * FROM " + DbNoteView.VIEW_NAME + " WHERE " + selection.toString() + " ORDER BY " + sortOrder;

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, sql, selectionArgs);
        return db.rawQuery(sql, selectionArgs.toArray(new String[selectionArgs.size()]));
    }

    private void searchQueryStates(StringBuilder selection, List<String> selectionArgs, String in, Set<String> states) {
        selection.append(" AND COALESCE(" + DbNoteView.STATE + ", '') ")
                .append(in).append(" (")
                .append(TextUtils.join(", ", Collections.nCopies(states.size(), "?")))
                .append(")");

        for (String state: states) {
            selectionArgs.add(state);
        }
    }

    private static void appendBeforeInterval(StringBuilder selection, String column, SearchQuery.SearchQueryInterval interval) {
        if (interval.none()) {
            selection.append(" AND ").append(column).append(" IS NULL");
            return;
        }

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
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString());

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            for (int i = 0; i < values.length; i++) {
                insertUnderTransaction(db, uri, values[i]);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyChange();

        return values.length;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString());

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        Uri resultUri;

        if (isInBatch()) {
            resultUri = insertUnderTransaction(db, uri, contentValues);

        } else {
            db.beginTransaction();
            try {
                resultUri = insertUnderTransaction(db, uri, contentValues);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            notifyChange();
        }

        return resultUri;
    }

    private Uri insertUnderTransaction(SQLiteDatabase db, Uri uri, ContentValues contentValues) {
        long id, noteId;
        String table;
        Uri resultUri;

        switch (uris.matcher.match(uri)) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                id = DbRepo.insert(db, contentValues.getAsString(ProviderContract.Repos.Param.REPO_URL));
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
                return insertNote(db, uri, contentValues, Place.UNSPECIFIED);

            case ProviderUris.NOTE_ABOVE:
                return insertNote(db, uri, contentValues, Place.ABOVE);

            case ProviderUris.NOTE_UNDER:
                return insertNote(db, uri, contentValues, Place.UNDER);

            case ProviderUris.NOTE_BELOW:
                return insertNote(db, uri, contentValues, Place.BELOW);

            case ProviderUris.NOTES_PROPERTIES:
                noteId = contentValues.getAsLong(ProviderContract.NoteProperties.Param.NOTE_ID);
                String name = contentValues.getAsString(ProviderContract.NoteProperties.Param.NAME);
                String value = contentValues.getAsString(ProviderContract.NoteProperties.Param.VALUE);
                int pos = contentValues.getAsInteger(ProviderContract.NoteProperties.Param.POSITION);

                long nameId = DbPropertyName.getOrInsert(db, name);
                long valueId = DbPropertyValue.getOrInsert(db, value);
                long propertyId = DbProperty.getOrInsert(db, nameId, valueId);
                long notePropertyId = DbNoteProperty.getOrInsert(db, noteId, pos, propertyId);

                return ContentUris.withAppendedId(uri, notePropertyId);

            case ProviderUris.LOAD_BOOK_FROM_FILE:
                resultUri = loadBookFromFile(contentValues);

                return resultUri;

            case ProviderUris.CURRENT_ROOKS:
                resultUri = insertCurrentRook(db, uri, contentValues);
                return resultUri;

            case ProviderUris.BOOKS_ID_SAVED:
                resultUri = bookSavedToRepo(db, uri, contentValues);
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

    private Uri insertNote(SQLiteDatabase db, Uri uri, ContentValues values, Place place) {
        NotePosition notePos = new NotePosition();

        long bookId = values.getAsLong(ProviderContract.Notes.UpdateParam.BOOK_ID);

        /* If new note is inserted relative to some other note, get info about that target note. */
        long refNoteId = 0;
        NotePosition refNotePos = null;
        if (place != Place.UNSPECIFIED) {
            refNoteId = Long.valueOf(uri.getPathSegments().get(1));
            refNotePos = DbNote.getPosition(db, refNoteId);
        }

        switch (place) {
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

            case UNSPECIFIED:
                /* If target note is not used, add note at the end with level 1. */
                long rootRgt = getMaxRgt(db, bookId);
                long rootId = getRootId(db, bookId);

                notePos.setLevel(1);
                notePos.setLft(rootRgt);
                notePos.setRgt(rootRgt + 1);
                notePos.setParentId(rootId);

                break;

            default:
                throw new IllegalArgumentException("Unsupported place for new note: " + place);
        }

        switch (place) {
            case ABOVE:
            case UNDER:
            case BELOW:
                /* Make space for new note - increment notes' LFT and RGT. */
                DatabaseUtils.makeSpaceForNewNotes(db, 1, refNotePos, place);

                /*
                 * If new note can be an ancestor, increment descendants count of all
                 * its ancestors.
                 */
                incrementDescendantsCountForAncestors(db, bookId, notePos.getLft(), notePos.getRgt());

            case UNSPECIFIED:
                /* Make space for new note - increment root's RGT. */
                String selection = DbNote.BOOK_ID + " = " + bookId + " AND " + DbNote.LFT + " = 1";
                GenericDatabaseUtils.incrementFields(db, DbNote.TABLE, selection, 2, ProviderContract.Notes.UpdateParam.RGT);
        }

        notePos.setBookId(bookId);

        DbNote.toContentValues(values, notePos);

        replaceTimestampRangeStringsWithIds(db, values);

        long id = db.insertOrThrow(DbNote.TABLE, null, values);

        db.execSQL("INSERT INTO " + DbNoteAncestor.TABLE +
                " (" + DbNoteAncestor.BOOK_ID + ", " +
                DbNoteAncestor.NOTE_ID +
                ", " + DbNoteAncestor.ANCESTOR_NOTE_ID + ") " +
                "SELECT " + field(DbNote.TABLE, DbNote.BOOK_ID) + ", " +
                field(DbNote.TABLE, DbNote._ID) + ", " +
                field("a", DbNote._ID) +
                " FROM " + DbNote.TABLE +
                " JOIN " + DbNote.TABLE + " a ON (" +
                field(DbNote.TABLE, DbNote.BOOK_ID) + " = " + field("a", DbNote.BOOK_ID) + " AND " +
                field("a", DbNote.LFT) + " < " + field(DbNote.TABLE, DbNote.LFT) + " AND " +
                field(DbNote.TABLE, DbNote.RGT) + " < " + field("a", DbNote.RGT) + ")" +
                " WHERE " + field(DbNote.TABLE, DbNote._ID) + " = " + id + " AND " + field("a", DbNote.LEVEL) + " > 0");

        return ContentUris.withAppendedId(uri, id);
    }

    private void incrementDescendantsCountForAncestors(SQLiteDatabase db, long bookId, long lft, long rgt) {
        db.execSQL("UPDATE " + DbNote.TABLE +
                " SET " + ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT + " = " + ProviderContract.Notes.UpdateParam.DESCENDANTS_COUNT + " + 1 " +
                "WHERE " + DatabaseUtils.whereAncestors(bookId, lft, rgt));
    }

    private int getMaxRgt(SQLiteDatabase db, long bookId) {
        Cursor cursor = db.query(
                DbNote.TABLE,
                new String[] { "MAX(" + DbNoteView.RGT + ")" },
                DbNoteView.BOOK_ID + "= " + bookId + " AND " + DbNoteView.IS_CUT + " = 0",
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
                DbNoteView.BOOK_ID + "= " + bookId + " AND " + DbNoteView.LEVEL + " = 0",
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
        v.put(DbCurrentVersionedRook.VERSIONED_ROOK_ID, versionedRookId);
        db.insert(DbCurrentVersionedRook.TABLE, null, v);

        return ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), bookId);
    }

    private static final String VERSIONED_ROOK_IDS_FOR_ROOK_ID =
            "SELECT _id FROM " + DbVersionedRook.TABLE + " WHERE " + DbVersionedRook.ROOK_ID + "=?";

    private static final String DELETE_CURRENT_VERSIONED_ROOKS_FOR_ROOK_ID =
            "DELETE FROM " + DbCurrentVersionedRook.TABLE +
                    " WHERE " + DbCurrentVersionedRook.VERSIONED_ROOK_ID + " IN (" + VERSIONED_ROOK_IDS_FOR_ROOK_ID + ")";

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
        values.put(DbCurrentVersionedRook.VERSIONED_ROOK_ID, versionedRookId);
        long id = db.insert(DbCurrentVersionedRook.TABLE, null, values);

        return ContentUris.withAppendedId(uri, id);
    }

    private long getOrInsertVersionedRook(SQLiteDatabase db, long rookId, String revision, long mtime) {
        long id = DatabaseUtils.getId(
                db,
                DbVersionedRook.TABLE,
                DbVersionedRook.ROOK_ID + "=? AND " + DbVersionedRook.ROOK_REVISION + "=? AND " + DbVersionedRook.ROOK_MTIME + "=?",
                new String[] { String.valueOf(rookId), revision, String.valueOf(mtime) });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(DbVersionedRook.ROOK_ID, rookId);
            values.put(DbVersionedRook.ROOK_REVISION, revision);
            values.put(DbVersionedRook.ROOK_MTIME, mtime);

            id = db.insertOrThrow(DbVersionedRook.TABLE, null, values);
        }

        return id;
    }

    private long getOrInsertRook(SQLiteDatabase db, long rookUrlId, long repoId) {
        long id = DatabaseUtils.getId(
                db,
                DbRook.TABLE,
                DbRook.ROOK_URL_ID + "=? AND " + DbRook.REPO_ID + "=?",
                new String[] { String.valueOf(rookUrlId), String.valueOf(repoId) });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(DbRook.ROOK_URL_ID, rookUrlId);
            values.put(DbRook.REPO_ID, repoId);

            id = db.insertOrThrow(DbRook.TABLE, null, values);
        }

        return id;
    }

    private long getOrInsertRepoUrl(SQLiteDatabase db, String repoUrl) {
        long id = DatabaseUtils.getId(
                db,
                DbRepo.TABLE,
                DbRepo.REPO_URL + "=?",
                new String[] { repoUrl });

        if (id == 0) {
            ContentValues values = new ContentValues();
            values.put(DbRepo.REPO_URL, repoUrl);
            values.put(DbRepo.IS_REPO_ACTIVE, 0);

            id = db.insertOrThrow(DbRepo.TABLE, null, values);
        }

        return id;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), selection, selectionArgs);

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int result;

        if (isInBatch()) {
            result = deleteUnderTransaction(db, uri, selection, selectionArgs);

        } else {
            db.beginTransaction();
            try {
                result = deleteUnderTransaction(db, uri, selection, selectionArgs);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            notifyChange();
        }

        return result;
    }

    private int deleteUnderTransaction(SQLiteDatabase db, Uri uri, String selection, String[] selectionArgs) {
        int result;
        long noteId;
        String table;

        switch (uris.matcher.match(uri)) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                result = DbRepo.delete(db, selection, selectionArgs);
                return result;

            /* Delete repo by just marking it as such. */
            case ProviderUris.REPOS_ID:
                selection = DbRepo._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;

                /* Remove books' links which are using this repo. */
                removeBooksLinksForRepo(db, uri.getLastPathSegment());

                /* Delete repo itself. */
                result = DbRepo.delete(db, selection, selectionArgs);

                return result;

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                break;

            case ProviderUris.FILTERS_ID:
                table = DbSearch.TABLE;
                selection = DbSearch._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.BOOKS:
                table = DbBook.TABLE;
                break;

            case ProviderUris.BOOKS_ID:
                table = DbBook.TABLE;
                selection = DbBook._ID + " = " + uri.getLastPathSegment();
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
                selection = DbBookLink.BOOK_ID + " = " + Long.parseLong(uri.getPathSegments().get(1));
                selectionArgs = null;
                result = db.delete(table, selection, selectionArgs);
                return result;

            case ProviderUris.NOTES_ID_PROPERTIES:
                noteId = Long.parseLong(uri.getPathSegments().get(1));

                if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.getQuery());

                table = DbNoteProperty.TABLE;

                selection = DbNoteProperty.NOTE_ID + " = " + noteId;
                selectionArgs = null;

                break;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        result = db.delete(table, selection, selectionArgs);

        return result;
    }

    private int removeBooksLinksForRepo(SQLiteDatabase db, String repoId) {
        /* Rooks which use passed repo. */
        String rookIds = "SELECT DISTINCT " + DbRook._ID +
                " FROM " + DbRook.TABLE +
                " WHERE " + DbRook.REPO_ID + " = " + repoId;

        return db.delete(DbBookLink.TABLE, DbBookLink.ROOK_ID + " IN (" + rookIds + ")", null);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString(), selection, selectionArgs);

        /* Used by tests. Open a different database so we don't overwrite user's real data. */
        if (uris.matcher.match(uri) == ProviderUris.DB_SWITCH) {
            reopenDatabaseWithDifferentName();
            return 1;
        }

        /* Gets a writable database. This will trigger its creation if it doesn't already exist. */
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int result;

        if (isInBatch()) {
            result = updateUnderTransaction(db, uri, contentValues, selection, selectionArgs);

        } else {
            db.beginTransaction();
            try {
                result = updateUnderTransaction(db, uri, contentValues, selection, selectionArgs);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            notifyChange();
        }

        return result;
    }

    /**
     * Re-open database with a different name (for tests), unless already using it.
     */
    private void reopenDatabaseWithDifferentName() {
        if (!DATABASE_NAME_FOR_TESTS.equals(mOpenHelper.getDatabaseName())) {
            mOpenHelper.close();
            mOpenHelper = new Database(getContext(), DATABASE_NAME_FOR_TESTS);
        }
    }

    private int updateUnderTransaction(SQLiteDatabase db, Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int result;
        long noteId;
        int match = uris.matcher.match(uri);

        String table;

        switch (match) {
            case ProviderUris.LOCAL_DB_REPO:
                table = DbDbRepo.TABLE;
                break;

            case ProviderUris.REPOS:
                result = DbRepo.update(db, contentValues, selection, selectionArgs);
                return result;

            case ProviderUris.REPOS_ID:
                selection = DbRepo._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                return DbRepo.update(db, contentValues, selection, selectionArgs);

            case ProviderUris.FILTERS:
                table = DbSearch.TABLE;
                break;

            case ProviderUris.FILTERS_ID:
                table = DbSearch.TABLE;
                selection = DbSearch._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.FILTER_UP:
                result = ProviderFilters.moveFilterUp(db, Long.parseLong(uri.getPathSegments().get(1)));
                return result;

            case ProviderUris.FILTER_DOWN:
                result = ProviderFilters.moveFilterDown(db, Long.parseLong(uri.getPathSegments().get(1)));
                return result;

            case ProviderUris.BOOKS:
                table = DbBook.TABLE;
                break;

            case ProviderUris.BOOKS_ID:
                table = DbBook.TABLE;
                selection = DbBook._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;
                break;

            case ProviderUris.NOTES:
                table = DbNote.TABLE;
                replaceTimestampRangeStringsWithIds(db, contentValues);
                break;

            case ProviderUris.NOTE:
                selection = DbNote._ID + " = " + uri.getLastPathSegment();
                selectionArgs = null;

                replaceTimestampRangeStringsWithIds(db, contentValues);

                result = db.update(DbNote.TABLE, contentValues, selection, selectionArgs);

                // TODO: Ugh: Use /books/1/notes/23/ or just move to constant
                if (uri.getQueryParameter("bookId") != null) {
                    DatabaseUtils.updateBookMtime(db, Long.parseLong(uri.getQueryParameter("bookId")));
                }

                return result;

            case ProviderUris.NOTES_STATE:
                String ids = contentValues.getAsString(ProviderContract.NotesState.Param.NOTE_IDS);
                String state = contentValues.getAsString(ProviderContract.NotesState.Param.STATE);

                result = setStateForNotes(db, ids, state);

                return result;

            case ProviderUris.CUT:
                return ActionRunner.run(db, new CutNotesAction(contentValues));

            case ProviderUris.PASTE:
                return ActionRunner.run(db, new PasteNotesAction(contentValues));

            case ProviderUris.DELETE:
                return ActionRunner.run(db, new DeleteNotesAction(contentValues));

            case ProviderUris.PROMOTE:
                return ActionRunner.run(db, new PromoteNotesAction(contentValues));

            case ProviderUris.DEMOTE:
                return ActionRunner.run(db, new DemoteNotesAction(contentValues));

            case ProviderUris.MOVE:
                return ActionRunner.run(db, new MoveNotesAction(contentValues));

            case ProviderUris.NOTE_TOGGLE_FOLDED_STATE:
                noteId = Long.valueOf(uri.getPathSegments().get(1));
                return ActionRunner.run(db, new ToggleFoldedStateAction(noteId));

            case ProviderUris.BOOKS_ID_CYCLE_VISIBILITY: {
                long bookId = Long.valueOf(uri.getPathSegments().get(1));
                return ActionRunner.run(db, new CycleVisibilityAction(bookId));
            }

            case ProviderUris.BOOKS_ID_SPARSE_TREE: {
                long bookId = Long.valueOf(uri.getPathSegments().get(1));
                return ActionRunner.run(db, new SparseTreeAction(bookId, contentValues));
            }

            case ProviderUris.LINKS_FOR_BOOK:
                return updateLinkForBook(db, uri, contentValues);

            case ProviderUris.DB_RECREATE:
                mOpenHelper.reCreateTables(db);

                return 0;

            default:
                throw new IllegalArgumentException("URI is not recognized: " + uri);
        }

        // FIXME: Hard to read - some cases above return, some are reaching this
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, table, contentValues, selection, selectionArgs);
        return db.update(table, contentValues, selection, selectionArgs);
    }


    /**
     * @param ids Note ids separated with comma
     * @param targetState keyword
     * @return Number of notes that have been updated
     */
    private int setStateForNotes(SQLiteDatabase db, String ids, String targetState) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, db, ids, targetState);

        int notesUpdated;

        /* Select only notes which don't already have the target state. */
        String notesSelection = DbNote._ID + " IN (" + ids + ") AND (" +
                DbNoteView.STATE + " IS NULL OR " + DbNoteView.STATE + " != ?)";

        String[] selectionArgs = { targetState };

        /* Select notebooks which will be affected. */
        String booksSelection = DbBook._ID + " IN (SELECT DISTINCT " +
                DbNote.BOOK_ID + " FROM " + DbNote.TABLE + " WHERE " + notesSelection + ")";

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

        return notesUpdated;
    }

    /**
     * Removes CLOSED timestamp and simply sets the state.
     */
    private int setOtherStateForNotes(SQLiteDatabase db, String targetState, String notesSelection, String[] selectionArgs) {
        ContentValues values = new ContentValues();
        values.put(DbNote.STATE, targetState);
        values.putNull(DbNote.CLOSED_RANGE_ID);

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
                DbNoteView.VIEW_NAME,
                new String[] {
                        DbNoteView._ID,
                        DbNoteView.SCHEDULED_RANGE_STRING,
                        DbNoteView.DEADLINE_RANGE_STRING,
                        DbNoteView.STATE,
                },
                notesSelection,
                selectionArgs,
                null,
                null,
                null);

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found notes: " + cursor.getCount());

        try {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                StateChangeLogic stateSetOp = new StateChangeLogic(AppPreferences.doneKeywordsSet(getContext()));

                stateSetOp.setState(state,
                        cursor.getString(3),
                        OrgRange.parseOrNull(cursor.getString(1)),
                        OrgRange.parseOrNull(cursor.getString(2)));

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

                notesUpdated += db.update(DbNote.TABLE, values, DbNote._ID + "=" + cursor.getString(0), null);
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
            db.delete(DbBookLink.TABLE, DbBookLink.BOOK_ID + "=" + bookId, null);
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
        values.put(DbBookLink.BOOK_ID, bookId);
        values.put(DbBookLink.ROOK_ID, rookId);

        long id = DatabaseUtils.getId(db, DbBookLink.TABLE, DbBookLink.BOOK_ID + "=" + bookId, null);
        if (id != 0) {
            db.update(DbBookLink.TABLE, values, DbBookLink._ID + "=" + id, null);
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
        values.put(DbBookSync.BOOK_VERSIONED_ROOK_ID, rookRevisionId);
        values.put(DbBookSync.BOOK_ID, bookId);

        long id = DatabaseUtils.getId(db, DbBookSync.TABLE, DbBookSync.BOOK_ID + "=" + bookId, null);
        if (id != 0) {
            db.update(DbBookSync.TABLE, values, DbBookSync._ID + "=" + id, null);
        } else {
            db.insertOrThrow(DbBookSync.TABLE, null, values);
        }

        return 1;
    }


    private Uri getOrInsertBook(SQLiteDatabase db, String name) {
        Cursor cursor = db.query(
                DbBook.TABLE,
                new String[] { DbBook._ID },
                DbBook.NAME + "=?",
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
        values.put(DbBook.NAME, name);

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
             * Determine encoding to use -- detect or force it.
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
        db.delete(DbNote.TABLE, DbNote.BOOK_ID + "=" + bookId, null);

        final Map<String, Long> propertyNames = new HashMap<>();
        {
            Cursor cursor = db.query(DbPropertyName.TABLE, new String[] {DbPropertyName._ID, DbPropertyName.NAME}, null, null, null, null, null);
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    propertyNames.put(cursor.getString(1), cursor.getLong(0));
                }
            } finally {
                cursor.close();
            }
        }

        final Map<String, Long> propertyValues = new HashMap<>();
        {
            Cursor cursor = db.query(DbPropertyValue.TABLE, new String[] {DbPropertyValue._ID, DbPropertyValue.VALUE}, null, null, null, null, null);
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    propertyValues.put(cursor.getString(1), cursor.getLong(0));
                }
            } finally {
                cursor.close();
            }
        }

        /*
         * Maps node's lft to database id.
         * Used to update parent id and insert ancestors.
         * Not using SparseArray as speed is preferred over memory here.
         */
        @SuppressLint("UseSparseArrays") final HashMap<Long,Long> lft2id = new HashMap<>();

        /* Set of ids for which parent is already set. */
        final Set<Long> notesWithParentSet = new HashSet<>();

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

                            ContentValues values = new ContentValues();

                            DbNote.toContentValues(values, position);
                            DbNote.toContentValues(db, values, node.getHead());

                            long noteId = db.insertOrThrow(DbNote.TABLE, null, values);

                            /* Insert note's properties. */
                            int pos = 1;
                            for (OrgProperty property: node.getHead().getProperties()) {
                                Long nameId = propertyNames.get(property.getName());
                                if (nameId == null) {
                                    nameId = DbPropertyName.getOrInsert(db, property.getName());
                                    propertyNames.put(property.getName(), nameId);
                                }

                                Long valueId = propertyValues.get(property.getValue());
                                if (valueId == null) {
                                    valueId = DbPropertyValue.getOrInsert(db, property.getValue());
                                    propertyValues.put(property.getValue(), valueId);
                                }

                                long propertyId = DbProperty.getOrInsert(db, nameId, valueId);

                                DbNoteProperty.getOrInsert(db, noteId, pos++, propertyId);
                            }

                            /*
                             * Update parent ID and insert ancestors.
                             * Going through all descendants - nodes between lft and rgt.
                             *
                             *  lft:  1    2    3    4    5   6
                             *            L2   l1   r2   R2
                             */
                            lft2id.put(node.getLft(), noteId);
                            for (long index = node.getLft() + 1; index < node.getRgt(); index++) {
                                Long descendantId = lft2id.get(index);
                                if (descendantId != null) {
                                    if (! notesWithParentSet.contains(descendantId)) {
                                        values = new ContentValues();
                                        values.put(DbNote.PARENT_ID, noteId);
                                        db.update(DbNote.TABLE, values, DbNote._ID + " = " + descendantId, null);
                                        notesWithParentSet.add(descendantId);
                                    }

                                    values = new ContentValues();
                                    values.put(DbNoteAncestor.NOTE_ID, descendantId);
                                    values.put(DbNoteAncestor.ANCESTOR_NOTE_ID, noteId);
                                    values.put(DbNoteAncestor.BOOK_ID, bookId);
                                    db.insert(DbNoteAncestor.TABLE, null, values);
                                }
                            }
                        }

                        @Override
                        public void onFile(OrgFile file) throws IOException {
                            BookSizeValidator.validate(file);

                            ContentValues values = new ContentValues();

                            BooksClient.toContentValues(values, file.getSettings());

                            /* Set preface. TODO: Move to and rename OrgFileSettings */
                            values.put(DbBook.PREFACE, file.getPreface());

                            values.put(DbBook.USED_ENCODING, usedEncoding);
                            values.put(DbBook.DETECTED_ENCODING, detectedEncoding);
                            values.put(DbBook.SELECTED_ENCODING, selectedEncoding);

                            db.update(DbBook.TABLE, values, DbBook._ID + "=" + bookId, null);
                        }

                    })
                    .build()
                    .parse();

        } finally {
            reader.close();
        }

        if (BuildConfig.LOG_DEBUG)
            LogUtils.d(TAG, bookName + ": Parsing done in " +
                    (System.currentTimeMillis() - startedAt) + " ms");

        if (rookUrl != null) {
            updateOrInsertBookLink(db, bookId, repoUrl, rookUrl);
            updateOrInsertBookSync(db, bookId, repoUrl, rookUrl, rookRevision, rookMtime);
        }

        /* Mark book as complete. */
        ContentValues values = new ContentValues();
        values.put(DbBook.IS_DUMMY, 0);
        db.update(DbBook.TABLE, values, DbBook._ID + "=" + bookId, null);

        return uri;
    }

    private void replaceTimestampRangeStringsWithIds(SQLiteDatabase db, ContentValues values) {

        if (values.containsKey(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.SCHEDULED_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)));
            } else {
                values.putNull(DbNote.SCHEDULED_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.SCHEDULED_STRING);
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.DEADLINE_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.DEADLINE_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)));
            } else {
                values.putNull(DbNote.DEADLINE_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.DEADLINE_STRING);
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.CLOSED_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.CLOSED_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.CLOSED_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)));
            } else {
                values.putNull(DbNote.CLOSED_RANGE_ID);
            }

            values.remove(ProviderContract.Notes.UpdateParam.CLOSED_STRING);
        }

        if (values.containsKey(ProviderContract.Notes.UpdateParam.CLOCK_STRING)) {
            String str = values.getAsString(ProviderContract.Notes.UpdateParam.CLOCK_STRING);
            if (! TextUtils.isEmpty(str)) {
                values.put(DbNote.CLOCK_RANGE_ID, getOrInsertOrgRange(db, OrgRange.parse(str)));
            } else {
                values.putNull(DbNote.CLOCK_RANGE_ID);
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

    private long getOrInsertOrgTime(SQLiteDatabase db, OrgDateTime orgDateTime) {
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

    private void notifyChange() {
        Uri uri = ProviderContract.AUTHORITY_URI;

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, uri.toString());
        getContext().getContentResolver().notifyChange(uri, null);
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
