package com.orgzly.android.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.orgzly.BuildConfig
import com.orgzly.android.AppIntent
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils
import org.joda.time.DateTime

object RemindersScheduler {
    fun scheduleReminder(context: Context, inMs: Long, hasTime: Boolean) {
        val intent = reminderTriggeredIntent(context)
        schedule(context, intent, inMs, hasTime)
    }

    @JvmStatic
    fun scheduleSnoozeEnd(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long, hasTime: Boolean) {
        val (inMs, newRunTime) = snoozeEndInMs(context, timestamp) ?: return
        val intent = snoozeEndedIntent(context, noteId, noteTimeType, newRunTime)
        schedule(context, intent, inMs, hasTime)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.cancel(reminderTriggeredIntent(context))
    }

    fun notifyDataSetChanged(context: Context) {
        context.sendBroadcast(dataSetChangedIntent(context))
    }

    private fun dataSetChangedIntent(context: Context): Intent {
        return Intent(context, RemindersBroadcastReceiver::class.java).apply {
            action = AppIntent.ACTION_REMINDER_DATA_CHANGED
        }
    }

    private fun reminderTriggeredIntent(context: Context): PendingIntent {
        return Intent(context, RemindersBroadcastReceiver::class.java).let { intent ->
            intent.action = AppIntent.ACTION_REMINDER_TRIGGERED
            PendingIntent.getBroadcast(context, 0, intent, ActivityUtils.immutable(0))
        }
    }

    private fun snoozeEndedIntent(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long): PendingIntent {
        return Intent(context, RemindersBroadcastReceiver::class.java).let { intent ->
            intent.action = AppIntent.ACTION_REMINDER_SNOOZE_ENDED
            intent.data = Uri.parse("custom://$noteId")

            intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
            intent.putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType)
            intent.putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp)

            PendingIntent.getBroadcast(context, 0, intent, ActivityUtils.immutable(0))
        }
    }

    private fun schedule(context: Context, intent: PendingIntent, inMs: Long, hasTime: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        if (alarmManager != null) {
            // TODO: Add preferences to control *how* to schedule the alarms
            if (hasTime) {
                // scheduleAlarmClock(alarmManager, intent, inMs)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scheduleExactAndAllowWhileIdle(alarmManager, intent, inMs)
                } else {
                    scheduleExact(alarmManager, intent, inMs)
                }
            } else {
                // Does not trigger while dozing
                scheduleExact(alarmManager, intent, inMs)
            }

            // Intent received, notifications not displayed by default
            // Note: Neither setAndAllowWhileIdle() nor setExactAndAllowWhileIdle() can fire
            // alarms more than once per 9 minutes, per app.
            // scheduleExactAndAllowWhileIdle(context, intent, inMs)

        } else {
            Log.e(TAG, "Failed getting AlarmManager")
            return
        }
    }

    private fun scheduleAlarmClock(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long) {
        val info = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + inMs, null)
        alarmManager.setAlarmClock(info, intent)
        logScheduled("setAlarmClock", inMs)
    }

    private fun scheduleExact(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long) {
        alarmManager.setExact(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + inMs,
            intent)
        logScheduled("setExact", inMs)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun scheduleExactAndAllowWhileIdle(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + inMs,
            intent)
        logScheduled("setExactAndAllowWhileIdle", inMs)
    }

    private fun logScheduled(s: String, inMs: Long) {
        if (BuildConfig.LOG_DEBUG) {
            val dateTime = DateTime(System.currentTimeMillis() + inMs).toString()
            LogUtils.d(TAG, "$s in $inMs ms ($dateTime) on API ${Build.VERSION.SDK_INT}")
        }
    }

    private fun snoozeEndInMs(context: Context, timestamp: Long): Pair<Long, Long>? {
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