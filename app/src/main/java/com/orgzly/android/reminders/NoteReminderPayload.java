package com.orgzly.android.reminders;

import com.orgzly.org.datetime.OrgDateTime;

class NoteReminderPayload {
    long noteId;
    long bookId;
    String bookName;
    String title;
    int timeType;
    OrgDateTime orgDateTime;

    NoteReminderPayload(long noteId, long bookId, String bookName, String title, int timeType, OrgDateTime orgDateTime) {
        this.noteId = noteId;
        this.bookId = bookId;
        this.bookName = bookName;
        this.title = title;
        this.timeType = timeType;
        this.orgDateTime = orgDateTime;
    }
}
