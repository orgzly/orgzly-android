package com.orgzly.android.reminders;

import com.orgzly.org.datetime.OrgDateTime;

import org.joda.time.DateTime;

class NoteReminder {
    DateTime triggerTime;

    long id;
    long bookId;
    String bookName;
    String title;
    OrgDateTime orgDateTime;

    NoteReminder(DateTime triggerTime, long id, long bookId, String bookName, String title, OrgDateTime orgDateTime) {
        this.triggerTime = triggerTime;

        this.id = id;
        this.bookId = bookId;
        this.bookName = bookName;
        this.title = title;
        this.orgDateTime = orgDateTime;
    }
}
