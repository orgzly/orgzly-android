package com.orgzly.android.reminders

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.orgzly.android.AppIntent
import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.getAlarmManager
import com.orgzly.android.util.LogMajorEvents
import org.joda.time.DateTime
import javax.inject.Inject

class RemindersScheduler @Inject constructor(val context: Application, val logs: AppLogsRepository) {

    companion object {
        fun notifyDataSetChanged(context: Context) {
            context.sendBroadcast(dataSetChangedIntent(context))
        }

        private fun dataSetChangedIntent(context: Context): Intent {
            return Intent(context, RemindersBroadcastReceiver::class.java).apply {
                action = AppIntent.ACTION_REMINDER_DATA_CHANGED
            }
        }
    }

    fun scheduleReminder(inMs: Long, hasTime: Boolean) {
        val intent = reminderTriggeredIntent()
        schedule(intent, inMs, hasTime, "reminder")
    }

    fun scheduleSnoozeEnd(noteId: Long, noteTimeType: Int, timestamp: Long, hasTime: Boolean) {
        val (inMs, newRunTime) = snoozeEndInMs(timestamp) ?: return
        val intent = snoozeEndedIntent(noteId, noteTimeType, newRunTime)
        schedule(intent, inMs, hasTime, "snooze")
    }

    fun cancelAll() {
        context.getAlarmManager().cancel(reminderTriggeredIntent())

        if (LogMajorEvents.isEnabled()) {
            logs.log(LogMajorEvents.REMINDERS, "Canceled all reminders")
        }
    }

    private fun reminderTriggeredIntent(): PendingIntent {
        return Intent(context, RemindersBroadcastReceiver::class.java).let { intent ->
            intent.action = AppIntent.ACTION_REMINDER_TRIGGERED
            PendingIntent.getBroadcast(context, 0, intent, ActivityUtils.immutable(0))
        }
    }

    private fun snoozeEndedIntent(noteId: Long, noteTimeType: Int, timestamp: Long): PendingIntent {
        return Intent(context, RemindersBroadcastReceiver::class.java).let { intent ->
            intent.action = AppIntent.ACTION_REMINDER_SNOOZE_ENDED
            intent.data = Uri.parse("custom://$noteId")

            intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
            intent.putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType)
            intent.putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp)

            PendingIntent.getBroadcast(context, 0, intent, ActivityUtils.immutable(0))
        }
    }

    private fun schedule(intent: PendingIntent, inMs: Long, hasTime: Boolean, origin: String) {
        val alarmManager = context.getAlarmManager()

        // TODO: Add preferences to control *how* to schedule the alarms
        if (hasTime) {
            if (AppPreferences.remindersUseAlarmClockForTodReminders(context)) {
                scheduleAlarmClock(alarmManager, intent, inMs, origin)

            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scheduleExactAndAllowWhileIdle(alarmManager, intent, inMs, origin)
                } else {
                    scheduleExact(alarmManager, intent, inMs, origin)
                }
            }

        } else {
            // Does not trigger while dozing
            scheduleExact(alarmManager, intent, inMs, origin)
        }

        // Intent received, notifications not displayed by default
        // Note: Neither setAndAllowWhileIdle() nor setExactAndAllowWhileIdle() can fire
        // alarms more than once per 9 minutes, per app.
        // scheduleExactAndAllowWhileIdle(context, intent, inMs)
    }

    private fun scheduleAlarmClock(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long, origin: String) {
        val info = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + inMs, null)
        alarmManager.setAlarmClock(info, intent)
        logScheduled("setAlarmClock", origin, inMs)
    }

    private fun scheduleExact(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long, origin: String) {
        alarmManager.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + inMs,
            intent)
        logScheduled("setExact", origin, inMs)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleExactAndAllowWhileIdle(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long, origin: String) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + inMs,
            intent)
        logScheduled("setExactAndAllowWhileIdle", origin, inMs)
    }

    private fun logScheduled(method: String, origin: String, inMs: Long) {
        if (LogMajorEvents.isEnabled()) {
            val now = System.currentTimeMillis()
            logs.log(
                LogMajorEvents.REMINDERS,
                "Scheduled ($origin) using $method in $inMs ms (~ ${DateTime(now + inMs)}) on ${Build.DEVICE} (API ${Build.VERSION.SDK_INT})"
            )
        }
    }

    private fun snoozeEndInMs(timestamp: Long): Pair<Long, Long>? {
        val snoozeTime = AppPreferences.remindersSnoozeTime(context) * 60 * 1000L
        val snoozeRelativeTo = AppPreferences.remindersSnoozeRelativeTo(context)

        when (snoozeRelativeTo) {
            "button" -> {
                // Fixed time after button is pressed
                return Pair(snoozeTime, timestamp)
            }

            "alarm" -> {
                var t = timestamp + snoozeTime
                var inMs = t - System.currentTimeMillis()
                // keep adding snooze times until positive: handle the case where
                // someone lets the alarm go off for more that one snoozeTime interval
                while (inMs <= 0) {
                    inMs += snoozeTime
                    t += snoozeTime
                }
                return Pair(inMs, t)
            }

            else -> {
                // should never happen
                Log.e(TAG, "unhandled snoozeRelativeTo $snoozeRelativeTo")
                return null
            }
        }
    }

    private val TAG: String = RemindersScheduler::class.java.name
}