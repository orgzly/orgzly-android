package com.orgzly.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_TIMEZONE_CHANGED
import android.content.Intent.ACTION_TIME_CHANGED
import com.orgzly.BuildConfig
import com.orgzly.android.data.DataRepository
import com.orgzly.android.usecase.TimestampRefresh
import com.orgzly.android.usecase.UseCaseRunner
import com.orgzly.android.util.LogUtils
import javax.inject.Inject

class TimeChangeBroadcastReceiver : BroadcastReceiver() {

    @Inject
    internal lateinit var dataRepository: DataRepository

    override fun onReceive(context: Context, intent: Intent) {
        App.appComponent.inject(this)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        when (intent.action) {
            ACTION_TIME_CHANGED -> onTimeChange()
            ACTION_TIMEZONE_CHANGED -> onTimezoneChange()
        }
    }

    private fun onTimezoneChange() {
        App.EXECUTORS.diskIO().execute {
            UseCaseRunner.run(TimestampRefresh())
        }
    }

//    private fun onTimezoneChange(context: Context) {
//        val prevTimeZone = AppPreferences.timeZone(context)?.let { TimeZone.getTimeZone(it) }
//
//        val currTimeZone = TimeZone.getDefault()
//
//        val now = System.currentTimeMillis()
//
//        // No previous time zone or time zone changed
//        if (prevTimeZone == null || prevTimeZone.getOffset(now) != currTimeZone.getOffset(now)) {
//        }
//
//        AppPreferences.timeZone(context, currTimeZone.id)
//    }

    private fun onTimeChange() {
    }

    companion object {
        private val TAG = TimeChangeBroadcastReceiver::class.java.name
    }
}