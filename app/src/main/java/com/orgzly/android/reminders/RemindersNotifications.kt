package com.orgzly.android.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.orgzly.R
import com.orgzly.android.AppIntent
import com.orgzly.android.NotificationBroadcastReceiver
import com.orgzly.android.NotificationChannels
import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.db.dao.ReminderTimeDao
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.ui.notifications.Notifications
import com.orgzly.android.ui.util.ActivityUtils
import com.orgzly.android.ui.util.ActivityUtils.mainActivityPendingIntent
import com.orgzly.android.ui.util.getNotificationManager
import com.orgzly.android.util.LogMajorEvents
import com.orgzly.android.util.OrgFormatter
import com.orgzly.android.util.UserTimeFormatter

object RemindersNotifications {
    val VIBRATION_PATTERN = longArrayOf(500, 50, 50, 300)

    private val LIGHTS = Triple(Color.BLUE, 1000, 5000)

    fun showNotifications(context: Context, notes: List<NoteReminder>, logs: AppLogsRepository) {
        val notificationManager = context.getNotificationManager()

        for (noteReminder in notes) {
            val wearableExtender = NotificationCompat.WearableExtender()

            val notificationTag = noteReminder.payload.noteId.toString()

            val content = getContent(context, noteReminder)

            val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setColor(ContextCompat.getColor(context, R.color.notification))
                    .setSmallIcon(R.drawable.cic_logo_for_notification)

            if (canGroupReminders()) {
                builder.setGroup(Notifications.REMINDERS_GROUP)
            }

            // Set vibration
            if (AppPreferences.remindersVibrate(context)) {
                builder.setVibrate(VIBRATION_PATTERN)
            }

            // Set notification sound
            if (AppPreferences.remindersSound(context)) {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }

            // Set LED
            if (AppPreferences.remindersLed(context)) {
                builder.setLights(LIGHTS.first, LIGHTS.second, LIGHTS.third)
            }

            builder.setContentTitle(OrgFormatter.parse(
                    noteReminder.payload.title,
                    context,
                    linkify = false,
                    parseCheckboxes = false
            ))

            builder.setContentText(content)

            builder.setStyle(NotificationCompat.InboxStyle()
                    .setSummaryText(noteReminder.payload.bookName)
                    .addLine(content)
            )

            // On click - open note
            builder.setContentIntent(mainActivityPendingIntent(
                    context, noteReminder.payload.bookId, noteReminder.payload.noteId))

            // Mark as done action - text depends on repeater
            val doneActionText = if (noteReminder.payload.orgDateTime.hasRepeater()) {
                context.getString(
                        R.string.mark_as_done_with_repeater,
                        noteReminder.payload.orgDateTime.repeater.toString())
            } else {
                context.getString(R.string.mark_as_done)
            }
            val markAsDoneAction = NotificationCompat.Action(R.drawable.ic_done,
                    doneActionText,
                    markNoteAsDonePendingIntent(
                            context, noteReminder.payload.noteId, notificationTag))
            builder.addAction(markAsDoneAction)
            wearableExtender.addAction(markAsDoneAction)

            // Snooze action
            val snoozeActionText = context.getString(R.string.reminder_snooze)
            val snoozeAction = NotificationCompat.Action(R.drawable.ic_snooze,
                    snoozeActionText,
                    reminderSnoozePendingIntent(
                            context,
                            noteReminder.payload.noteId,
                            noteReminder.payload.timeType,
                            noteReminder.runTime.millis,
                            notificationTag))
            builder.addAction(snoozeAction)
            wearableExtender.addAction(snoozeAction)

            builder.extend(wearableExtender)

            notificationManager.notify(notificationTag, Notifications.REMINDER_ID, builder.build())

            if (LogMajorEvents.isEnabled()) {
                val note = "\"${noteReminder.payload.title}\" (id:${noteReminder.payload.noteId})"

                logs.log(
                    LogMajorEvents.REMINDERS,
                    "Notified (tag:$notificationTag id:${Notifications.REMINDER_ID}): $note"
                )
            }
        }

        // Create a group summary notification, but only if notifications can be grouped
        if (canGroupReminders()) {
            if (notes.isNotEmpty()) {
                val builder = NotificationCompat.Builder(context, NotificationChannels.REMINDERS)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.cic_logo_for_notification)
                        .setGroup(Notifications.REMINDERS_GROUP)
                        .setGroupSummary(true)

                notificationManager.notify(Notifications.REMINDERS_SUMMARY_ID, builder.build())
            }
        }
    }

    private fun getContent(context: Context, noteReminder: NoteReminder): String {
        val resId = when (noteReminder.payload.timeType) {
            ReminderTimeDao.SCHEDULED_TIME ->
                R.string.reminder_for_scheduled

            ReminderTimeDao.DEADLINE_TIME ->
                R.string.reminder_for_deadline

            else ->
                R.string.reminder_for_event
        }


        val timeStr = UserTimeFormatter(context).formatAll(noteReminder.payload.orgDateTime)

        return context.getString(resId, timeStr)
    }

    // Create a group summary notification, but only if notifications be grouped and expanded
    private fun canGroupReminders(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    private fun markNoteAsDonePendingIntent(
            context: Context, noteId: Long, tag: String
    ): PendingIntent {

        val intent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            action = AppIntent.ACTION_NOTE_MARK_AS_DONE
            putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
            putExtra(AppIntent.EXTRA_NOTIFICATION_TAG, tag)
            putExtra(AppIntent.EXTRA_NOTIFICATION_ID, Notifications.REMINDER_ID)
        }

        return PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            ActivityUtils.immutable(PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun reminderSnoozePendingIntent(
            context: Context, noteId: Long, noteTimeType: Int, timestamp: Long, tag: String
    ): PendingIntent {

        val intent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            action = AppIntent.ACTION_REMINDER_SNOOZE_REQUESTED
            putExtra(AppIntent.EXTRA_NOTE_ID, noteId)
            putExtra(AppIntent.EXTRA_NOTE_TIME_TYPE, noteTimeType)
            putExtra(AppIntent.EXTRA_SNOOZE_TIMESTAMP, timestamp)
            putExtra(AppIntent.EXTRA_NOTIFICATION_TAG, tag)
            putExtra(AppIntent.EXTRA_NOTIFICATION_ID, Notifications.REMINDER_ID)
        }

        return PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            ActivityUtils.immutable(PendingIntent.FLAG_UPDATE_CURRENT))
    }
}