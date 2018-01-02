package com.orgzly.android.provider.clients;

import android.content.Context;
import android.database.Cursor;

import com.orgzly.android.provider.ProviderContract;

public class TimesClient {
    private static final String TAG = TimesClient.class.getName();

    public static void forEachTime(Context context, TimesClientInterface listener) {
        Cursor cursor = context.getContentResolver().query(
                ProviderContract.Times.ContentUri.times(), null, null, null, null);

        if (cursor != null) {
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    listener.onTime(new NoteTime(
                            cursor.getLong(ProviderContract.Times.ColumnIndex.NOTE_ID),
                            cursor.getLong(ProviderContract.Times.ColumnIndex.BOOK_ID),
                            cursor.getString(ProviderContract.Times.ColumnIndex.BOOK_NAME),
                            cursor.getString(ProviderContract.Times.ColumnIndex.NOTE_STATE),
                            cursor.getString(ProviderContract.Times.ColumnIndex.NOTE_TITLE),
                            cursor.getInt(ProviderContract.Times.ColumnIndex.TIME_TYPE),
                            cursor.getString(ProviderContract.Times.ColumnIndex.ORG_TIMESTAMP_STRING)
                    ));
                }
            } finally {
                cursor.close();
            }
        }
    }

    public interface TimesClientInterface {
        void onTime(NoteTime time);
    }

    public static class NoteTime {
        public long noteId;
        public long bookId;
        public String bookName;
        public String state;
        public String title;
        public int timeType;
        public String orgTimestampString;

        NoteTime(long noteId, long bookId, String bookName, String state, String title, int timeType, String orgTimestampString) {
            this.noteId = noteId;
            this.bookId = bookId;
            this.bookName = bookName;
            this.state = state;
            this.title = title;
            this.timeType = timeType;
            this.orgTimestampString = orgTimestampString;
        }
    }
}