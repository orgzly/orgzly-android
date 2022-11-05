package com.orgzly.android.prefs


import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TimePicker
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat


class TimePreferenceFragment : PreferenceDialogFragmentCompat() {
    private lateinit var timePicker: TimePicker

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        // Get the time from the related Preference
        val minutesAfterMidnight = (preference as TimePreference).getTime()

        // Set the time to the TimePicker
        timePicker = view.findViewById<TimePicker>(com.orgzly.R.id.time_picker_layout).apply {
            val hours = minutesAfterMidnight / 60
            val minutes = minutesAfterMidnight % 60
            val is24hour = DateFormat.is24HourFormat(context)

            setIs24HourView(is24hour)

            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hour = hours
                minute = minutes
            } else {
                currentHour = hours
                currentMinute = minutes
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            @Suppress("DEPRECATION")
            val hours = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.hour
            } else {
                timePicker.currentHour
            }

            @Suppress("DEPRECATION")
            val minutes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                timePicker.minute
            } else {
                timePicker.currentMinute
            }

            val minutesAfterMidnight = hours * 60 + minutes

            // Get the related Preference and save the value
            val preference = preference
            if (preference is TimePreference) {
                // This allows the client to ignore the user value.
                if (preference.callChangeListener(minutesAfterMidnight)) {
                    // Save the value
                    preference.setTime(minutesAfterMidnight)
                }
            }

            preference.summary = minutesAfterMidnight.toString()
        }
    }

    companion object {
        val FRAGMENT_TAG: String = TimePreferenceFragment::class.java.name

        fun getInstance(preference: Preference): PreferenceDialogFragmentCompat {
            return TimePreferenceFragment().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_KEY, preference.key)
                }
            }
        }
    }
}
