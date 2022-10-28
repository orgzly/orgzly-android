package com.orgzly.android.ui

import android.os.Bundle
import android.os.SystemClock
import com.orgzly.R
import com.orgzly.android.reminders.LastRun
import com.orgzly.android.ui.util.copyPlainTextToClipboard
import com.orgzly.android.ui.util.getAlarmManager
import com.orgzly.android.ui.util.sharePlainText
import com.orgzly.android.ui.util.userFriendlyPeriod
import com.orgzly.android.util.LogMajorEvents
import com.orgzly.databinding.ActivityLogsBinding
import org.joda.time.DateTime

class LogsActivity : CommonActivity() {
    private lateinit var binding: ActivityLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLogsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.info.setTextIsSelectable(true)
        binding.logs.setTextIsSelectable(true)

        updateViewsWithFreshData()

        binding.topToolbar.run {
            setNavigationOnClickListener {
                finish()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.refresh ->
                        updateViewsWithFreshData()
                    R.id.copy ->
                        copyPlainTextToClipboard("Orgzly Logs", allText())
                    R.id.share ->
                        sharePlainText(allText())
                }

                true
            }
        }
    }

    private fun updateViewsWithFreshData() {
        binding.info.text = getInfo()
        binding.logs.text = getLogs()
    }

    private fun allText(): CharSequence {
        return binding.info.text.toString() + "\n" + binding.logs.text.toString()
    }

    private fun getLogs(): CharSequence {
        return LogMajorEvents.readLogFile()
    }

    private fun getInfo(): CharSequence {
        val currentTime = DateTime.now()

        val bootElapsedMs = SystemClock.elapsedRealtime()

        val bootAt = currentTime - bootElapsedMs

        val bootElapsed = bootElapsedMs.userFriendlyPeriod()

        val nextAlarmClock = getAlarmManager().nextAlarmClock.let { alarmClockInfo ->
            if (alarmClockInfo != null) {
                DateTime(alarmClockInfo.triggerTime)
            } else {
                "Not set"
            }
        }

        val lastRun = LastRun.fromPreferences(this)

        return """
                Now
                $currentTime

                Next alarm clock (on device)
                $nextAlarmClock

                Last run for reminders
                Scheduled
                ${lastRun.scheduled}
                Deadline
                ${lastRun.deadline}
                Event
                ${lastRun.event}

                Last boot (including deep sleep)
                $bootAt
                $bootElapsed
            """.trimIndent()
    }
}