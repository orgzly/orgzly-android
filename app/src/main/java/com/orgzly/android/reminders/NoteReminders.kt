package com.orgzly.android.reminders

import android.content.Context
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.dao.ReminderTimeDao
import com.orgzly.android.db.dao.ReminderTimeDao.NoteTime
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgDateTimeUtils
import com.orgzly.org.datetime.OrgInterval
import org.joda.time.DateTime
import org.joda.time.ReadableInstant
import java.util.*


object NoteReminders {
    // private val TAG: String = NoteReminders::class.java.name

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
                    AppPreferences.reminderDailyTime(context),
                    warningPeriod
                )

//                    if (BuildConfig.LOG_DEBUG) {
//                        LogUtils.d(TAG,
//                                "Note's time", noteTime,
//                                "Interval", interval,
//                                "Found first time", time)
//                    }

                if (time != null) {
                    result.add(NoteReminder(time, payload))
                }
            }
        }

        // Sort by time, older first
        result.sortWith { o1, o2 -> o1.runTime.compareTo(o2.runTime) }

        return result
    }

    fun isRelevantNoteTime(context: Context, noteTime: NoteTime): Boolean {
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
        defaultTimeOfDay: Int,
        warningPeriod: OrgInterval?): DateTime? {

        val times = OrgDateTimeUtils.getTimesInInterval(
            orgDateTime,
            interval.first,
            interval.second,
            defaultTimeOfDay,
            false, // Do not use repeater for reminders
            warningPeriod,
            1)

        return times.firstOrNull()
    }
}