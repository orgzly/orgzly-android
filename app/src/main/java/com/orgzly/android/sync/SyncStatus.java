package com.orgzly.android.sync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.orgzly.android.AppIntent;

public class SyncStatus {
    private static final String SHARED_PREF_NAME = "sync-service";

    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_TOTAL_BOOKS = "total_books";
    private static final String EXTRA_CURRENT_BOOK = "current_book";

    public Type type = Type.NOT_RUNNING;
    public String message = null;
    public int totalBooks = 0;
    public int currentBook = 0;

    public static SyncStatus fromIntent(Intent intent) {
        SyncStatus status = new SyncStatus();

        status.type = SyncStatus.Type.valueOf(intent.getStringExtra(SyncStatus.EXTRA_TYPE));
        status.message = intent.getStringExtra(SyncStatus.EXTRA_MESSAGE);
        status.currentBook = intent.getIntExtra(SyncStatus.EXTRA_CURRENT_BOOK, 0);
        status.totalBooks = intent.getIntExtra(SyncStatus.EXTRA_TOTAL_BOOKS, 0);

        return status;
    }

    public void set(Type type, String message, int currentBook, int totalBooks) {
        this.type = type;
        this.message = message;
        this.currentBook = currentBook;
        this.totalBooks = totalBooks;
    }

    public Intent intent() {
        return new Intent(AppIntent.ACTION_SYNC_STATUS)
                .putExtra(SyncStatus.EXTRA_TYPE, type.name())
                .putExtra(SyncStatus.EXTRA_MESSAGE, message)
                .putExtra(SyncStatus.EXTRA_TOTAL_BOOKS, totalBooks)
                .putExtra(SyncStatus.EXTRA_CURRENT_BOOK, currentBook);
    }

    public void saveToPreferences(Context context) {
        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(EXTRA_TYPE, type.toString())
                .putString(EXTRA_MESSAGE, message)
                .putInt(EXTRA_CURRENT_BOOK, currentBook)
                .putInt(EXTRA_TOTAL_BOOKS, totalBooks)
                .apply();
    }

    public void loadFromPreferences(Context context) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        this.type = Type.valueOf(pref.getString(EXTRA_TYPE, Type.NOT_RUNNING.toString()));
        this.message = pref.getString(EXTRA_MESSAGE, null);
        this.currentBook = pref.getInt(EXTRA_CURRENT_BOOK, 0);
        this.totalBooks = pref.getInt(EXTRA_TOTAL_BOOKS, 0);
    }

    public enum Type {
        NOT_RUNNING,
        STARTING,
        CANCELING,
        BOOKS_COLLECTED,
        BOOK_STARTED,
        BOOK_ENDED,
        NO_STORAGE_PERMISSION,
        CANCELED,
        FINISHED,
        FAILED
    }
}
