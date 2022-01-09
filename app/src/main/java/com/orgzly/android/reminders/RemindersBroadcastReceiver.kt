package com.orgzly.android.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.LogUtils
import com.orgzly.android.util.async
import com.orgzly.org.datetime.OrgDateTime
import org.joda.time.DateTime
import java.util.*
import javax.inject.Inject

class RemindersBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var dataRepository: DataRepository

    override fun onReceive(context: Context, intent: Intent) {
        App.appComponent.inject(this)

        if (!areRemindersEnabled(context)) {
            return
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent, intent.action, intent.extras)

        async {
            val now = DateTime()
            val lastRun = LastRun.fromPreferences(context)

            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "now:$now lastRun: $lastRun")

            RemindersScheduler.cancelAll(context)

            when (intent.action) {
                Intent.ACTION_BOOT_COMPLETED,
                AppIntent.ACTION_REMINDER_DATA_CHANGED -> {
                    // Nothing to do, just schedule the next alert below
                }

                AppIntent.ACTION_REMINDER_TRIGGERED -> {
                    reminderTriggered(context, now, lastRun)
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

            scheduleNextReminder(context, now, lastRun)

            LastRun.toPreferences(context, now)
        }
    }

    private fun areRemindersEnabled(context: Context): Boolean {
        return if (AppPreferences.remindersForScheduledEnabled(context)
            || AppPreferences.remindersForDeadlineEnabled(context)
            || AppPreferences.remindersForEventsEnabled(context)) {
            true
        } else {
            logAction("All reminders are disabled")
            false
        }
    }

    /**
     * Display reminders for all notes with times between previous run and now.
     */
    private fun reminderTriggered(context: Context, now: DateTime, lastRun: LastRun?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val msg = if (lastRun != null) {
            val reminders = NoteReminders.getNoteReminders(
                context, dataRepository, now, lastRun, NoteReminders.TIME_BEFORE_NOW)

            if (reminders.isNotEmpty()) {
                RemindersNotifications.showNotification(context, reminders)

                "Found ${reminders.size} notes between $lastRun and $now"

            } else {
                "No notes found between $lastRun and $now"
            }

        } else {
            "No previous run"
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg)
    }

    /**
     * Schedule the next job for times after now.
     */
    private fun scheduleNextReminder(context: Context, now: DateTime, lastRun: LastRun) {
        val notes = NoteReminders.getNoteReminders(
            context, dataRepository, now, lastRun, NoteReminders.TIME_FROM_NOW)

        if (notes.isNotEmpty()) {
            // Schedule only the first upcoming time
            val firstNote = notes[0]

            // Schedule *in* exactMs
            var inMs = firstNote.runTime.millis - now.millis
            if (inMs < 0) {
                inMs = 1
            }

            RemindersScheduler.cancelAll(context)
            RemindersScheduler.scheduleReminder(context, inMs)

            logForDebuggingScheduled(inMs, firstNote.payload.title)

        } else {
            logAction("No notes found")
        }
    }

    private fun logForDebuggingScheduled(exactMs: Long, title: String) {
        val log = String.format(
            Locale.getDefault(),
            "In %d sec for note \"%s\"",
            exactMs / 1000,
            title)

        logAction(log)
    }

    private fun snoozeEnded(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp)

        val reminders = mutableListOf<NoteReminder>()

        for (noteTime in dataRepository.times()) {
            if (noteTime.noteId == noteId
                && noteTime.timeType == noteTimeType
                && NoteReminders.isRelevantNoteTime(context, noteTime)) {

                val orgDateTime = OrgDateTime.parse(noteTime.orgTimestampString)

                val payload = NoteReminderPayload(
                    noteTime.noteId,
                    noteTime.bookId,
                    noteTime.bookName,
                    noteTime.title,
                    noteTime.timeType,
                    orgDateTime)

                val timestampDateTime = DateTime(timestamp)

                reminders.add(NoteReminder(timestampDateTime, payload))
            }
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "Found ${reminders.size} notes")

        if (reminders.isNotEmpty()) {
            RemindersNotifications.showNotification(context, reminders)
        }
    }

    /**
     * Display notification on every attempt to schedule a reminder.
     * Used for debugging.
     */
    private fun logAction(msg: String) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg)

        //        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
//                .setCategory(NotificationCompat.CATEGORY_REMINDER)
//                .setPriority(NotificationCompat.PRIORITY_MAX)
//                .setColor(ContextCompat.getColor(this, R.color.notification))
//                .setSmallIcon(R.drawable.cic_orgzly_notification)
//                .setContentTitle("Next reminder")
//                .setContentText(msg);
//
//        notificationManager.notify(Notifications.REMINDER_SCHEDULED, builder.build());
    }

    companion object {
        private val TAG: String = RemindersBroadcastReceiver::class.java.name
    }
}