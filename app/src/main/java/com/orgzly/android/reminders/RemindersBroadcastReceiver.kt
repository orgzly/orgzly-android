package com.orgzly.android.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.userFriendlyPeriod
import com.orgzly.android.util.LogMajorEvents
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.async
import com.orgzly.org.datetime.OrgDateTime
import org.joda.time.DateTime
import javax.inject.Inject

class RemindersBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    lateinit var appLogs: AppLogsRepository

    @Inject
    lateinit var remindersScheduler: RemindersScheduler

    override fun onReceive(context: Context, intent: Intent) {
        App.appComponent.inject(this)

        if (!anyRemindersEnabled(context, intent)) {
            return
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        async {
            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                AppIntent.ACTION_REMINDER_DATA_CHANGED,
                AppIntent.ACTION_REMINDER_TRIGGERED -> {
                    val now = DateTime()
                    val lastRun = LastRun.fromPreferences(context)

                    remindersScheduler.cancelAll()

                    notifyForRemindersSinceLastRun(context, now, lastRun)

                    scheduleNextReminder(context, now, lastRun)
                    LastRun.toPreferences(context, now)
                }

                AppIntent.ACTION_REMINDER_SNOOZE_ENDED -> {
                    intent.extras?.apply {
                        val noteId: Long = getLong(AppIntent.EXTRA_NOTE_ID, 0)
                        val noteTimeType: Int = getInt(AppIntent.EXTRA_NOTE_TIME_TYPE, 0)
                        val timestamp: Long = getLong(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0)

                        if (noteId > 0) {
                            snoozeEnded(context, noteId, noteTimeType, timestamp)
                        }
                    }
                }
            }

        }
    }

    private fun anyRemindersEnabled(context: Context, intent: Intent): Boolean {
        return if (AppPreferences.remindersForScheduledEnabled(context)) {
            if (LogMajorEvents.isEnabled()) {
                appLogs.log(
                    LogMajorEvents.REMINDERS,
                    "Intent accepted - scheduled time reminder is enabled: $intent"
                )
            }
            true
        } else if (AppPreferences.remindersForDeadlineEnabled(context)) {
            if (LogMajorEvents.isEnabled()) {
                appLogs.log(
                    LogMajorEvents.REMINDERS,
                    "Intent accepted - deadline time reminder is enabled: $intent"
                )
            }
            true
        } else if (AppPreferences.remindersForEventsEnabled(context)) {
            if (LogMajorEvents.isEnabled()) {
                appLogs.log(
                    LogMajorEvents.REMINDERS,
                    "Intent accepted - events reminder is enabled: $intent"
                )
            }
            true
        } else {
            if (LogMajorEvents.isEnabled()) {
                appLogs.log(
                    LogMajorEvents.REMINDERS,
                    "Intent ignored - all reminders are disabled: $intent"
                )
            }
            false
        }
    }

    /**
     * Display reminders for all notes with times between previous run and now.
     */
    private fun notifyForRemindersSinceLastRun(context: Context, now: DateTime, lastRun: LastRun?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        if (lastRun != null) {
            val notes = NoteReminders.getNoteReminders(
                context, dataRepository, now, lastRun, NoteReminders.INTERVAL_FROM_LAST_TO_NOW)

            if (notes.isNotEmpty()) {
                // TODO: Show less, show summary
                val lastNotes = notes.takeLast(20)

                if (LogMajorEvents.isEnabled()) {
                    appLogs.log(
                        LogMajorEvents.REMINDERS,
                        "Since last run: Found ${notes.size} notes (showing ${lastNotes.size}) between $lastRun and $now")
                }

                RemindersNotifications.showNotifications(context, lastNotes, appLogs)

            } else {
                if (LogMajorEvents.isEnabled()) {
                    appLogs.log(
                        LogMajorEvents.REMINDERS,
                        "Since last run: No notes found between $lastRun and $now")
                }
            }

        } else {
            if (LogMajorEvents.isEnabled()) {
                appLogs.log(LogMajorEvents.REMINDERS, "Since last run: No previous run")
            }
        }
    }

    /**
     * Schedule the next job for times after now.
     */
    private fun scheduleNextReminder(context: Context, now: DateTime, lastRun: LastRun) {
        val notes = NoteReminders.getNoteReminders(
            context, dataRepository, now, lastRun, NoteReminders.INTERVAL_FROM_NOW)

        if (notes.isNotEmpty()) {
            // Schedule only the first upcoming time
            val firstNote = notes.first()

            val id = firstNote.payload.noteId
            val title = firstNote.payload.title
            val runAt = firstNote.runTime.millis
            val hasTime = firstNote.payload.orgDateTime.hasTime()

            // Schedule in this many milliseconds
            var inMs = runAt - now.millis
            if (inMs < 0) {
                inMs = 1
            }

            if (LogMajorEvents.isEnabled()) {
                val inS = inMs.userFriendlyPeriod()
                appLogs.log(
                    LogMajorEvents.REMINDERS,
                    "Next: Found ${notes.size} notes from $now and scheduling first in $inS ($inMs ms): \"$title\" (id:$id)"
                )
            }

            remindersScheduler.scheduleReminder(inMs, hasTime)

        } else {
            if (LogMajorEvents.isEnabled()) {
                appLogs.log(
                    LogMajorEvents.REMINDERS, "Next: No notes found from $now"
                )
            }
        }
    }

    private fun snoozeEnded(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp)

        val reminders = mutableListOf<NoteReminder>()

        for (noteTime in dataRepository.times()) {
            if (noteTime.noteId == noteId
                && noteTime.timeType == noteTimeType
                && NoteReminders.isRelevantNoteTime(context, noteTime)) {

                val orgDateTime = OrgDateTime.parse(noteTime.orgTimestampString)

                val timestampDateTime = DateTime(timestamp)

                val payload = NoteReminderPayload(
                    noteTime.noteId,
                    noteTime.bookId,
                    noteTime.bookName,
                    noteTime.title,
                    noteTime.timeType,
                    orgDateTime)

                reminders.add(NoteReminder(timestampDateTime, payload))
            }
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found ${reminders.size} notes")

        if (reminders.isNotEmpty()) {
            RemindersNotifications.showNotifications(context, reminders, appLogs)
        }
    }

    companion object {
        private val TAG: String = RemindersBroadcastReceiver::class.java.name
    }
}