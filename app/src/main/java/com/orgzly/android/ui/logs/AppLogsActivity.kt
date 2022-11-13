package com.orgzly.android.ui.logs

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.orgzly.R
import com.orgzly.android.App
import com.orgzly.android.data.logs.AppLogsRepository
import com.orgzly.android.reminders.LastRun
import com.orgzly.android.ui.CommonActivity
import com.orgzly.android.ui.util.copyPlainTextToClipboard
import com.orgzly.android.ui.util.getAlarmManager
import com.orgzly.android.ui.util.sharePlainText
import com.orgzly.android.ui.util.userFriendlyPeriod
import com.orgzly.databinding.ActivityLogsBinding
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import javax.inject.Inject

class AppLogsActivity : CommonActivity() {
    private lateinit var binding: ActivityLogsBinding

    private lateinit var viewModel: AppLogsViewModel

    @Inject
    lateinit var appLogs: AppLogsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        App.appComponent.inject(this)

        super.onCreate(savedInstanceState)

        binding = ActivityLogsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.info.setTextIsSelectable(true)
        binding.logs.setTextIsSelectable(true)


        val factory = AppLogsViewModelFactory.getInstance(appLogs)
        viewModel = ViewModelProvider(this, factory)[AppLogsViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logs.collect {
                    binding.logs.text = it.joinToString("\n")
                }
            }
        }

        updateInfoWithFreshData()

        binding.topToolbar.run {
            setNavigationOnClickListener {
                finish()
            }

            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.refresh ->
                        updateInfoWithFreshData()
                    R.id.copy ->
                        copyPlainTextToClipboard("Orgzly Logs", allText())
                    R.id.share ->
                        sharePlainText(allText())
                }

                true
            }
        }
    }

    private fun updateInfoWithFreshData() {
        binding.info.text = getInfo()
    }

    private fun allText(): CharSequence {
        return binding.info.text.toString() + "\n" + binding.logs.text.toString()
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