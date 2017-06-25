package com.orgzly.android.reminders;

import org.joda.time.DateTime;

class NoteReminder {
    private DateTime runTime;
    private NoteReminderPayload payload;

    NoteReminder(DateTime runTime, NoteReminderPayload payload) {
        this.runTime = runTime;
        this.payload = payload;
    }

    public DateTime getRunTime() {
        return runTime;
    }

    public NoteReminderPayload getPayload() {
        return payload;
    }
}