package com.orgzly.android.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.orgzly.BuildConfig
import com.orgzly.android.AppIntent
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.util.LogUtils

class ReminderAlarmManager : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent, intent.action, intent.extras)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                ReminderService.notifyDataSetChanged(context)
            }

            AppIntent.ACTION_REMINDER_TRIGGERED -> {
                ReminderService.reminderTriggered(context)
            }

            AppIntent.ACTION_REMINDER_SNOOZE_ENDED -> {
                intent.extras?.apply {
                    val noteId: Long = getLong(AppIntent.EXTRA_NOTE_ID, 0)
                    val noteTimeType: Int = getInt(AppIntent.EXTRA_NOTE_TIME_TYPE, 0)
                    val timestamp: Long = getLong(AppIntent.EXTRA_SNOOZE_TIMESTAMP, 0)
                    ReminderService.snoozeEnded(context, noteId, noteTimeType, timestamp)
                }
            }
        }
    }

    companion object {
        fun scheduleNextReminder(context: Context, inMs: Long) {
            val intent = reminderTriggeredIntent(context)
            schedule(context, intent, inMs)
        }

        fun cancelAll(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(reminderTriggeredIntent(context))
        }

        private fun reminderTriggeredIntent(context: Context): PendingIntent {
            return Intent(context, ReminderAlarmManager::class.java).let { intent ->
                intent.action = AppIntent.ACTION_REMINDER_TRIGGERED
                PendingIntent.getBroadcast(context, 0, intent, ActivityUtils.immutable(0))
            }
        }

        @JvmStatic
        fun scheduleSnoozeEnd(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long) {
            val (inMs, newRunTime) = snoozeEndInMs(context, timestamp) ?: return
            val intent = snoozeEndedIntent(context, noteId, noteTimeType, newRunTime)
            schedule(context, intent, inMs)
        }

        private fun snoozeEndedIntent(context: Context, noteId: Long, noteTimeType: Int, timestamp: Long): PendingIntent {
            return Intent(context, ReminderAlarmManager::class.java).let { intent ->
                intent.action = AppIntent.ACTION_REMINDER_SNOOZE_ENDED
                intent.data = Uri.parse("custom://$noteId")

                intent.putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
                intent.putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType)
                intent.putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp)

                PendingIntent.getBroadcast(context, 0, intent, ActivityUtils.immutable(0))
            }
        }

        private fun schedule(context: Context, intent: PendingIntent, inMs: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

            if (alarmManager != null) {
                val wakeUpDevice = true // AppPreferences.(context)

                if (wakeUpDevice) {
                    // Doesn't always immediately show notifications
                    scheduleAlarmClock(alarmManager, intent, inMs)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val info = AlarmManager.AlarmClockInfo(System.currentTimeMillis() + inMs, null)
                alarmManager.setAlarmClock(info, intent)

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "setAlarmClock in $inMs ms (API ${Build.VERSION.SDK_INT})")

            } else {
                scheduleExact(alarmManager, intent, inMs)
            }
        }

        private fun scheduleExactAndAllowWhileIdle(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + inMs,
                    intent)

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "setExactAndAllowWhileIdle in $inMs ms (API ${Build.VERSION.SDK_INT})")

            } else {
                scheduleExact(alarmManager, intent, inMs)
            }
        }

        private fun scheduleExact(alarmManager: AlarmManager, intent: PendingIntent, inMs: Long) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + inMs,
                    intent)

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "setExact in $inMs ms (API ${Build.VERSION.SDK_INT})")

            } else {
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + inMs,
                    intent)

                if (BuildConfig.LOG_DEBUG)
                    LogUtils.d(TAG, "set in $inMs ms (API ${Build.VERSION.SDK_INT})")
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

        private val TAG: String = ReminderAlarmManager::class.java.name

    }
}