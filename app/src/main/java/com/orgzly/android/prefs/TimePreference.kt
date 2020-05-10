package com.orgzly.android.prefs

import android.content.Context
import android.content.res.TypedArray
import android.text.format.DateFormat
import android.util.AttributeSet

import androidx.preference.DialogPreference
import com.orgzly.R
import org.joda.time.DateTime
import org.joda.time.DateTimeZone

class TimePreference : DialogPreference {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        dialogLayoutResource = R.layout.pref_dialog_time
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        dialogLayoutResource = R.layout.pref_dialog_time
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        dialogLayoutResource = R.layout.pref_dialog_time
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override fun getSummary(): CharSequence {
        val timeFormat = DateFormat.getTimeFormat(context)

        val time = DateTime.now(DateTimeZone.forTimeZone(timeFormat.timeZone))
                .withTimeAtStartOfDay()
                .plusMinutes(getTime())
                .toDate()

        return timeFormat.format(time)
    }

    fun getTime(): Int {
        return AppPreferences.reminderDailyTime(context)
    }

    fun setTime(time: Int) {
        AppPreferences.reminderDailyTime(context, time)
    }
}
