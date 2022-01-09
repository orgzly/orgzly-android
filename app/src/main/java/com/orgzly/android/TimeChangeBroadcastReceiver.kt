package com.orgzly.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orgzly.BuildConfig
import com.orgzly.android.data.DataRepository
import com.orgzly.android.usecase.TimestampUpdate
import com.orgzly.android.usecase.UseCaseWorker
import com.orgzly.android.util.LogUtils
import javax.inject.Inject

class TimeChangeBroadcastReceiver : BroadcastReceiver() {

    @Inject
    internal lateinit var dataRepository: DataRepository

    override fun onReceive(context: Context, intent: Intent) {
        App.appComponent.inject(this)

        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        when (intent.action) {
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                UseCaseWorker.schedule(context, TimestampUpdate())
            }
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



    companion object {
        private val TAG = TimeChangeBroadcastReceiver::class.java.name
    }
}