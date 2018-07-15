package com.orgzly.android.provider.clients;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.orgzly.R;
import com.orgzly.android.Book;
import com.orgzly.android.BookAction;
import com.orgzly.android.BookName;
import com.orgzly.android.prefs.AppPreferences;
import com.orgzly.android.provider.ProviderContract;
import com.orgzly.android.provider.actions.SparseTreeAction;
import com.orgzly.android.provider.views.DbBookView;
import com.orgzly.android.repos.Rook;
import com.orgzly.android.repos.VersionedRook;
import com.orgzly.android.util.ExceptionUtils;
import com.orgzly.org.OrgFileSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BooksClient {
    /** Sort by modification times. */
    private static final String ORDER_BY_MTIME =
            ProviderContract.Books.Param.IS_DUMMY + "," +
                    "MAX(COALESCE("+ DbBookView.VIEW_NAME+"."+ProviderContract.Books.Param.MTIME+", 0), COALESCE("+ DbBookView.VIEW_NAME+"."+ProviderContract.Books.Param.SYNCED_ROOK_MTIME+", 0)) DESC, " +
                    ProviderContract.Books.Param.NAME;

    /** Sort by title or filename. */
    private static final String ORDER_BY_NAME =
            ProviderContract.Books.Param.IS_DUMMY + "," +
                    "LOWER(COALESCE(" + DbBookView.VIEW_NAME+"."+ProviderContract.Books.Param.TITLE + ", "
                    + DbBookView.VIEW_NAME+"."+ProviderContract.Books.Param.NAME + "))";

    private static void toContentValues(ContentValues values, Book book) {
        values.put(ProviderContract.Books.Param.NAME, book.getName());
        values.put(ProviderContract.Books.Param.PREFACE, book.getPreface());
        values.put(ProviderContract.Books.Param.MTIME, book.getMtime());
        values.put(ProviderContract.Books.Param.IS_DUMMY, book.isDummy() ? 1 : 0);

        if (book.getSyncStatus() != null) {
            values.put(ProviderContract.Books.Param.SYNC_STATUS, book.getSyncStatus().toString());
        } else {
            values.putNull(ProviderContract.Books.Param.SYNC_STATUS);
        }

        toContentValues(values, book.getOrgFileSettings());
    }

    public static void toContentValues(ContentValues values, OrgFileSettings settings) {
        /* Set title. */
        if (settings.getTitle() != null) {
            values.put(ProviderContract.Books.Param.TITLE, settings.getTitle());
        } else {
            values.putNull(ProviderContract.Books.Param.TITLE);
        }

        values.put(ProviderContract.Books.Param.IS_INDENTED, settings.isIndented() ? 1 : 0);
    }

    public static Book fromCursor(Cursor cursor) {
        Book book = new Book(
                cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.PREFACE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.MTIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.IS_DUMMY)) == 1
        );

        book.getOrgFileSettings().setTitle(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.TITLE)));
        book.getOrgFileSettings().setIndented(cursor.getInt(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.IS_INDENTED)) == 1);

        book.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param._ID)));
        book.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SYNC_STATUS)));

        book.setDetectedEncoding(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.DETECTED_ENCODING)));
        book.setSelectedEncoding(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SELECTED_ENCODING)));
        book.setUsedEncoding(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.USED_ENCODING)));

        /* Set link. */
        int linkRepoUriColumn = cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.LINK_REPO_URL);
        if (! cursor.isNull(linkRepoUriColumn)) {
            Uri uri = Uri.parse(cursor.getString(linkRepoUriColumn));
            book.setLinkRepo(uri);
        }

        /* Set versioned rook. */
        if (! cursor.isNull(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SYNCED_ROOK_URL))) {
            Uri syncRookUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SYNCED_ROOK_URL)));
            Uri syncRepoUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SYNCED_REPO_URL)));
            String rev = cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SYNCED_ROOK_REVISION));
            long mtime = cursor.getLong(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.SYNCED_ROOK_MTIME));

            VersionedRook vrook = new VersionedRook(syncRepoUri, syncRookUri, rev, mtime);

            book.setLastSyncedToRook(vrook);
        }

        /* Set last action. */
        String lastActionMessage = cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.LAST_ACTION));
        if (! TextUtils.isEmpty(lastActionMessage)) {
            BookAction.Type lastActionType = BookAction.Type.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.LAST_ACTION_TYPE)));
            long lastActionTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(ProviderContract.Books.Param.LAST_ACTION_TIMESTAMP));

            BookAction lastAction = new BookAction(lastActionType, lastActionMessage, lastActionTimestamp);

            book.setLastAction(lastAction);
        }

        return book;
    }

    /**
     * @throws IOException if notebook with the same name already exists or some other failure.
     */
    public static Book insert(Context context, Book book) throws IOException {
        if (doesExist(context, book.getName())) {
            throw new IOException("Can't insert notebook with the same name: " + book.getName());
        }

        ContentValues values = new ContentValues();
        BooksClient.toContentValues(values, book);
        Uri uri;
        try {
            uri = context.getContentResolver().insert(ProviderContract.Books.ContentUri.books(), values);
        } catch (Exception e) {
            throw ExceptionUtils.IOException(e, "Failed inserting book " + book.getName());
        }
        book.setId(ContentUris.parseId(uri));

        return book;
    }

    /**
     * Update book's modified time.
     */
    public static int setModifiedTime(Context context, long bookId, long time) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Books.Param.MTIME, time);

        return context.getContentResolver().update(ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), bookId), values, null, null);
    }

    /**
     * Update book's link URL.
     */
    public static int setLink(Context context, long bookId, String repoUrl) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.BookLinks.Param.REPO_URL, repoUrl);

        return context.getContentResolver().update(ProviderContract.BookLinks.ContentUri.booksIdLinks(bookId), values, null, null);
    }

    public static int removeLink(Context context, long bookId) {
        return context.getContentResolver().delete(ProviderContract.BookLinks.ContentUri.booksIdLinks(bookId), null, null);
    }

    /**
     * Deletes a single book.
     *
     * @param id row ID of the book to delete
     */
    public static void delete(Context context, long id) {
        context.getContentResolver().delete(ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), id), null, null);
    }

    public static List<Book> getAll(Context context) {
        List<Book> books = new ArrayList<>();

        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Books.ContentUri.books(), null, null, null, getSortOrder(context))) {
            if (cursor != null) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    books.add(fromCursor(cursor));
                }
            }
        }

        return books;
    }


    /**
     * Get book from database by its ID.
     *
     * @param bookId ID for the {@link Book}
     * @return {@link Book} or {@code null} if the book with specified ID doesn't exist
     */
    public static Book get(Context context, long bookId) {
        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Books.ContentUri.booksId(bookId), null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor);
            } else {
                return null;
            }
        }
    }

    /**
     * Get book from database by its name.
     *
     * @param name Name of the book
     * @return {@link Book} or {@code null} if the book with specified name doesn't exist
     */
    public static Book get(Context context, String name) {

        try (Cursor cursor = context.getContentResolver().query(
                ProviderContract.Books.ContentUri.books(),
                null,
                ProviderContract.Books.Param.NAME + "=?",
                new String[]{name},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor);
            } else {
                return null;
            }
        }
    }

    /** Checks if notebook with the same name already exists in database. */
    public static boolean doesExist(Context context, String name) {
        try (Cursor cursor = context.getContentResolver().query(ProviderContract.Books.ContentUri.books(), null, ProviderContract.Books.Param.NAME + " = ?", new String[]{name}, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }

    /**
     * Stores synchronization message to database, prepended with current time.
     */
    public static int updateStatus(Context context, long bookId, String status, BookAction action) {
        ContentValues values = new ContentValues();

        // TODO: Do we even need status in DB? Is it used except for tests?
        if (status != null) {
            values.put(ProviderContract.Books.Param.SYNC_STATUS, status);
        } else {
            values.putNull(ProviderContract.Books.Param.SYNC_STATUS);
        }

        values.put(ProviderContract.Books.Param.LAST_ACTION, action.getMessage());
        values.put(ProviderContract.Books.Param.LAST_ACTION_TIMESTAMP, action.getTimestamp());
        values.put(ProviderContract.Books.Param.LAST_ACTION_TYPE, action.getType().toString());

        return context.getContentResolver().update(ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), bookId), values, null, null);
    }

    public static int updateSettings(Context context, Book book) {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.Books.Param.PREFACE, book.getPreface());
        values.put(ProviderContract.Books.Param.TITLE, book.getOrgFileSettings().getTitle());
        values.put(ProviderContract.Books.Param.MTIME, System.currentTimeMillis());

        return context.getContentResolver().update(
                ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), book.getId()), values, null, null);
    }

    public static int updatePreface(Context context, long bookId, String preface) {
        ContentValues values = new ContentValues();

        values.put(ProviderContract.Books.Param.PREFACE, preface);
        values.put(ProviderContract.Books.Param.MTIME, System.currentTimeMillis());

        return context.getContentResolver().update(
                ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), bookId), values, null, null);
    }

    public static int setModificationTime(Context context, long id, long time) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Books.Param.MTIME, time);
        return context.getContentResolver().update(
                ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), id),
                values, null, null);
    }

    public static int updateName(Context context, long id, String name) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Books.Param.NAME, name);
        return context.getContentResolver().update(ContentUris.withAppendedId(ProviderContract.Books.ContentUri.books(), id), values, null, null);
    }

    public static int promote(Context context, long bookId, Set<Long> noteIds) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Promote.Param.BOOK_ID, bookId);
        values.put(ProviderContract.Promote.Param.IDS, TextUtils.join(",", noteIds));

        return context.getContentResolver().update(ProviderContract.Promote.ContentUri.promote(), values, null, null);
    }

    public static int demote(Context context, long bookId, Set<Long> noteIds) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Demote.Param.BOOK_ID, bookId);
        values.put(ProviderContract.Demote.Param.IDS, TextUtils.join(",", noteIds));

        return context.getContentResolver().update(ProviderContract.Demote.ContentUri.demote(), values, null, null);
    }

    public static void cycleVisibility(Context context, Book book) {
        context.getContentResolver().update(ProviderContract.Books.ContentUri.booksIdCycleVisibility(book.getId()), null, null, null);
    }

    public static int moveNotes(Context context, long bookId, Long noteId, int direction) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.Move.Param.BOOK_ID, bookId);
        values.put(ProviderContract.Move.Param.IDS, noteId);
        values.put(ProviderContract.Move.Param.DIRECTION, direction);

        return context.getContentResolver().update(ProviderContract.Move.ContentUri.move(), values, null, null);
    }

    public static Uri loadFromFile(Context context, String name, BookName.Format format, File file, VersionedRook vrook, String selectedEncoding) throws IOException {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.LoadBookFromFile.Param.BOOK_NAME, name);
        values.put(ProviderContract.LoadBookFromFile.Param.FORMAT, format.toString());
        values.put(ProviderContract.LoadBookFromFile.Param.FILE_PATH, file.getAbsolutePath());

        if (vrook != null) {
            values.put(ProviderContract.LoadBookFromFile.Param.ROOK_REPO_URL, vrook.getRepoUri().toString());
            values.put(ProviderContract.LoadBookFromFile.Param.ROOK_URL, vrook.getUri().toString());
            values.put(ProviderContract.LoadBookFromFile.Param.ROOK_REVISION, vrook.getRevision());
            values.put(ProviderContract.LoadBookFromFile.Param.ROOK_MTIME, vrook.getMtime());
        }

        if (selectedEncoding != null) {
            values.put(ProviderContract.LoadBookFromFile.Param.SELECTED_ENCODING, selectedEncoding);
        }

        try {
            return context.getContentResolver().insert(ProviderContract.LoadBookFromFile.ContentUri.loadBookFromFile(), values);
        } catch (IllegalArgumentException e) {
            throw ExceptionUtils.IOException(e, "Failed loading book " + name);
            // FIXME: We sometimes catch these exceptions from content provider, sometimes not
        }
    }

    public static void saved(Context context, long id, VersionedRook uploadedBook) {
        ContentValues values = new ContentValues();
        values.put(ProviderContract.BooksIdSaved.Param.REPO_URL, uploadedBook.getRepoUri().toString());
        values.put(ProviderContract.BooksIdSaved.Param.ROOK_URL, uploadedBook.getUri().toString());
        values.put(ProviderContract.BooksIdSaved.Param.ROOK_REVISION, uploadedBook.getRevision());
        values.put(ProviderContract.BooksIdSaved.Param.ROOK_MTIME, uploadedBook.getMtime());

        context.getContentResolver().insert(ProviderContract.BooksIdSaved.ContentUri.booksIdSaved(id), values);
    }

    public static Loader<Cursor> getCursorLoader(Context context) {
        return new CursorLoader(
                context,
                ProviderContract.Books.ContentUri.books(),
                DbBookView.PROJECTION,
                null,
                null,
                getSortOrder(context));
    }

    private static String getSortOrder(Context context) {
        String sortOrder = ORDER_BY_NAME;

        if (context.getString(R.string.pref_value_notebooks_sort_order_modification_time).equals(AppPreferences.notebooksSortOrder(context))) {
            sortOrder = ORDER_BY_MTIME;
        }

        return sortOrder;
    }

    public static void sparseTree(Context context, long bookId, long noteId) {
        ContentValues values = new ContentValues();
        values.put(SparseTreeAction.ID, noteId);

        context.getContentResolver().update(ProviderContract.Books.ContentUri.booksIdSparseTree(bookId), values, null, null);
    }
}
