package com.orgzly.android.reminders

import org.joda.time.DateTime

data class NoteReminder(val runTime: DateTime, val payload: NoteReminderPayload)