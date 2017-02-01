package com.orgzly.android.sync;

public class SyncStatus {
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_TOTAL_BOOKS = "total_books";
    public static final String EXTRA_CURRENT_BOOK = "current_book";

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

    public Type type = Type.NOT_RUNNING;

    public String message = null;

    public int totalBooks = 0;
    public int currentBook = 0;


    public void set(Type type, String message, int currentBook, int totalBooks) {
        this.type = type;
        this.message = message;
        this.currentBook = currentBook;
        this.totalBooks = totalBooks;
    }
}
