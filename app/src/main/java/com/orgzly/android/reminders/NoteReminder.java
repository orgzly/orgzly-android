package com.orgzly.android.reminders;

import org.joda.time.DateTime;

class NoteReminder {
    private DateTime runTime;
    private NoteReminderPayload payload;

    NoteReminder(DateTime runTime, NoteReminderPayload payload) {
        this.runTime = runTime;
        this.payload = payload;
    }

    DateTime getRunTime() {
        return runTime;
    }

    NoteReminderPayload getPayload() {
        return payload;
    }
}