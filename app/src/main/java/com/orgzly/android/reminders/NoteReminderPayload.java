package com.orgzly.android.reminders;

import com.orgzly.org.datetime.OrgDateTime;

public class NoteReminderPayload {
    long id;
    long bookId;
    String bookName;
    String title;
    OrgDateTime orgDateTime;

    NoteReminderPayload(long id, long bookId, String bookName, String title, OrgDateTime orgDateTime) {
        this.id = id;
        this.bookId = bookId;
        this.bookName = bookName;
        this.title = title;
        this.orgDateTime = orgDateTime;
    }
}
