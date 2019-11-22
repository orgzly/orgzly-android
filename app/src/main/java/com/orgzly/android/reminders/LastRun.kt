package com.orgzly.android.reminders

import android.content.Context
import com.orgzly.android.prefs.AppPreferences
import org.joda.time.DateTime

data class LastRun(
        val scheduled: DateTime? = null,
        val deadline: DateTime? = null,
        val event: DateTime? = null) {

    companion object {
        @JvmStatic
        fun fromPreferences(context: Context): LastRun {
            fun Long.toDateTime() = if (this > 0) DateTime(this) else null

            return LastRun(
                    AppPreferences.reminderLastRunForScheduled(context).toDateTime(),
                    AppPreferences.reminderLastRunForDeadline(context).toDateTime(),
                    AppPreferences.reminderLastRunForEvents(context).toDateTime())
        }

        @JvmStatic
        fun toPreferences(context: Context, now: DateTime) {
            if (AppPreferences.remindersForScheduledEnabled(context)) {
                AppPreferences.reminderLastRunForScheduled(context, now.millis)
            }

            if (AppPreferences.remindersForDeadlineEnabled(context)) {
                AppPreferences.reminderLastRunForDeadline(context, now.millis)
            }

            if (AppPreferences.remindersForEventsEnabled(context)) {
                AppPreferences.reminderLastRunForEvents(context, now.millis)
            }
        }
    }
}
