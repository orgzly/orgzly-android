package com.orgzly.android.reminders

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.evernote.android.job.JobManager
import com.orgzly.BuildConfig
import com.orgzly.android.App
import com.orgzly.android.AppIntent
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.dao.ReminderTimeDao
import com.orgzly.android.db.dao.ReminderTimeDao.NoteTime
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.LogUtils
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgDateTimeUtils
import com.orgzly.org.datetime.OrgInterval
import org.joda.time.DateTime
import org.joda.time.ReadableInstant
import java.util.*
import javax.inject.Inject

/**
 * Every event that can affect reminders is going through this service.
 */
class ReminderService : JobIntentService() {
    @Inject
    lateinit var dataRepository: DataRepository

    override fun onCreate() {
        App.appComponent.inject(this)

        super.onCreate()
    }

    override fun onHandleWork(intent: Intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        if (!areRemindersEnabled()) {
            return
        }

        val event = getEvent(intent) ?: return

        val now = DateTime()

        val lastRun = LastRun.fromPreferences(this)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, "$event Now:$now $lastRun")

        ReminderJob.cancelAll()

        when (event) {
            Event.DATA_CHANGED -> {
                // Only schedule the next job below
            }

            Event.JOB_TRIGGERED ->
                onJobTriggered(now, lastRun)

            Event.SNOOZE_JOB_TRIGGERED -> {
                val noteId = intent.getLongExtra(AppIntent.EXTRA_NOTE_ID, 0)
                val noteTimeType = intent.getIntExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, 0)
                val timestamp = intent.getLongExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0)

                if (noteId > 0) {
                    onSnoozeTriggered(this, noteId, noteTimeType, timestamp)
                }
            }
        }

        scheduleNextJob(now, lastRun)

        LastRun.toPreferences(this, now)
    }

    private fun areRemindersEnabled(): Boolean {
        return if (AppPreferences.remindersForScheduledEnabled(this)
                || AppPreferences.remindersForDeadlineEnabled(this)
                || AppPreferences.remindersForEventsEnabled(this)) {
            true
        } else {
            logAction("All reminders are disabled")
            false
        }
    }

    private fun getEvent(intent: Intent): Event? {
        val name = intent.getStringExtra(AppIntent.EXTRA_REMINDER_EVENT) ?: return null

        return try {
            Event.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unknown event ($name) received")
            null
        }
    }

    /**
     * Schedule the next job for times after now.
     */
    private fun scheduleNextJob(now: DateTime, lastRun: LastRun) {
        val notes = getNoteReminders(this, dataRepository, now, lastRun, TIME_FROM_NOW)

        if (notes.isNotEmpty()) {
            // Schedule only the first upcoming time
            val firstNote = notes[0]

            // Schedule *in* exactMs
            var exactMs = firstNote.runTime.millis - now.millis
            if (exactMs < 0) {
                exactMs = 1
            }

            val jobId = ReminderJob.scheduleJob(exactMs)

            logForDebuggingScheduled(jobId, exactMs, firstNote.payload.title)

        } else {
            logAction("No notes found")
        }
    }

    private fun logForDebuggingScheduled(jobId: Int, exactMs: Long, title: String) {
        val jobRequest = JobManager.instance().getJobRequest(jobId)
        val runTime = jobRequest.scheduledAt + jobRequest.startMs
        val jobRunTimeString = DateTime(runTime).toString()

        val log = String.format(
                Locale.getDefault(),
                "#%d in %d sec (%s) for note \"%s\"",
                jobId,
                exactMs / 1000,
                jobRunTimeString,
                title)

        logAction(log)
    }

    /**
     * Display reminders for all notes with times between previous run and now.
     */
    private fun onJobTriggered(now: DateTime, lastRun: LastRun?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG)

        val msg = if (lastRun != null) {
            val reminders = getNoteReminders(this, dataRepository, now, lastRun, TIME_BEFORE_NOW)

            if (reminders.isNotEmpty()) {
                ReminderNotifications.showNotification(this, reminders)

                "Found ${reminders.size} notes between $lastRun and $now"

            } else {
                "No notes found between $lastRun and $now"
            }

        } else {
            "No previous run"
        }

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, msg)
    }

    private fun onSnoozeTriggered(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp)

        val reminders = mutableListOf<NoteReminder>()

        for (noteTime in dataRepository.times()) {
            if (noteTime.noteId == noteId
                    && noteTime.timeType == noteTimeType
                    && isRelevantNoteTime(context, noteTime)) {

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
            ReminderNotifications.showNotification(this, reminders)
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

    private enum class Event {
        DATA_CHANGED,
        JOB_TRIGGERED,
        SNOOZE_JOB_TRIGGERED
    }

    companion object {
        val TAG: String = ReminderService::class.java.name

        const val TIME_BEFORE_NOW = 1
        const val TIME_FROM_NOW = 2

        @JvmStatic
        fun getNoteReminders(
                context: Context,
                dataRepository: DataRepository,
                now: ReadableInstant,
                lastRun: LastRun,
                beforeOrAfter: Int): List<NoteReminder> {

            val result: MutableList<NoteReminder> = ArrayList()

            for (noteTime in dataRepository.times()) {
                if (isRelevantNoteTime(context, noteTime)) {
                    val orgDateTime = OrgDateTime.parse(noteTime.orgTimestampString)

                    val payload = NoteReminderPayload(
                            noteTime.noteId,
                            noteTime.bookId,
                            noteTime.bookName,
                            noteTime.title,
                            noteTime.timeType,
                            orgDateTime)

                    val interval = intervalToConsider(beforeOrAfter, now, lastRun, noteTime.timeType)

                    // Deadline warning period

                    val warningPeriod = if (isWarningPeriodSupported(noteTime)) {
                        if (orgDateTime.hasDelay()) {
                            orgDateTime.delay as OrgInterval
                        } else {
                            // TODO: Use default from user preference
                            // OrgInterval(1, OrgInterval.Unit.DAY)
                            null
                        }
                    } else {
                        null
                    }

                    val time = getFirstTime(
                            orgDateTime,
                            interval,
                            OrgInterval(9, OrgInterval.Unit.HOUR),  // Default time of day
                            warningPeriod
                    )

                    if (time != null) {
                        result.add(NoteReminder(time, payload))
                    }
                }
            }

            // Sort by time, older first
            result.sortWith(Comparator { o1, o2 -> o1.runTime.compareTo(o2.runTime) })

            return result
        }

        private fun isRelevantNoteTime(context: Context, noteTime: NoteTime): Boolean {
            val doneStateKeywords = AppPreferences.doneKeywordsSet(context)
            val isDone = doneStateKeywords.contains(noteTime.state)

            val isEnabled = AppPreferences.remindersForScheduledEnabled(context)
                    && noteTime.timeType == ReminderTimeDao.SCHEDULED_TIME
                    || AppPreferences.remindersForDeadlineEnabled(context)
                    && noteTime.timeType == ReminderTimeDao.DEADLINE_TIME
                    || AppPreferences.remindersForEventsEnabled(context)
                    && noteTime.timeType == ReminderTimeDao.EVENT_TIME

            return isEnabled && !isDone
        }

        private fun isWarningPeriodSupported(noteTime: NoteTime): Boolean {
            return noteTime.timeType == ReminderTimeDao.DEADLINE_TIME
                    || noteTime.timeType == ReminderTimeDao.EVENT_TIME
        }

        /**
         * Notify reminder service about changes that might affect scheduling of reminders.
         */
        fun notifyForDataChanged(context: Context) {
            val intent = Intent(context, ReminderService::class.java).apply {
                putExtra(AppIntent.EXTRA_REMINDER_EVENT, Event.DATA_CHANGED.name)
            }

            enqueueWork(context, intent)
        }

        /**
         * Notify ReminderService about the triggered job.
         */
        @JvmStatic
        fun notifyForJobTriggered(context: Context) {
            val intent = Intent(context, ReminderService::class.java).apply {
                putExtra(AppIntent.EXTRA_REMINDER_EVENT, Event.JOB_TRIGGERED.name)
            }

            enqueueWork(context, intent)
        }

        /**
         * Notify ReminderService about the triggered snooze job.
         */
        @JvmStatic
        fun notifyForSnoozeTriggered(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long) {
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp)

            val intent = Intent(context, ReminderService::class.java).apply {
                putExtra(AppIntent.EXTRA_REMINDER_EVENT, Event.SNOOZE_JOB_TRIGGERED.name)
                putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
                putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType)
                putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp)
            }

            enqueueWork(context, intent)
        }

        private fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(context, ReminderService::class.java, App.REMINDER_SERVICE_JOB_ID, intent)
        }

        private fun intervalToConsider(
                beforeOrAfter: Int, now: ReadableInstant, lastRun: LastRun, timeType: Int
        ): Pair<ReadableInstant, ReadableInstant?> {

            when (beforeOrAfter) {
                TIME_BEFORE_NOW -> {
                    val from = when (timeType) {
                        ReminderTimeDao.SCHEDULED_TIME -> {
                            lastRun.scheduled
                        }
                        ReminderTimeDao.DEADLINE_TIME -> {
                            lastRun.deadline
                        }
                        else -> {
                            lastRun.event
                        }
                    }

                    return Pair(from ?: now, now)
                }

                TIME_FROM_NOW -> {
                    return Pair(now, null)
                }

                else -> throw IllegalArgumentException("Before or after now?")
            }
        }

        private fun getFirstTime(
                orgDateTime: OrgDateTime,
                interval: Pair<ReadableInstant, ReadableInstant?>,
                defaultTimeOfDay: OrgInterval,
                warningPeriod: OrgInterval?): DateTime? {

            val times = OrgDateTimeUtils.getTimesInInterval(
                    orgDateTime, interval.first, interval.second, false, warningPeriod, 1)

            if (times.isEmpty()) {
                return null
            }
            var time = times[0]
            if (!orgDateTime.hasTime()) {
                // TODO: Move to preferences
                time = time.plusHours(9)
            }
            return time
        }
    }
}