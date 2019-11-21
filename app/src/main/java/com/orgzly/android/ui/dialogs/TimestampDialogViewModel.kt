package com.orgzly.android.ui.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.TimeType
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgDelay
import com.orgzly.org.datetime.OrgInterval
import com.orgzly.org.datetime.OrgRepeater
import java.util.*


class TimestampDialogViewModel(val timeType: TimeType, orgDateTime: String?) : CommonViewModel() {

    data class DateTime(
            val year: Int,
            val month: Int,
            val day: Int,
            val isTimeUsed: Boolean,
            val hour: Int,
            val minute: Int,
            val isEndTimeUsed: Boolean,
            val endHour: Int,
            val endMinute: Int,
            val isRepeaterUsed: Boolean,
            val repeater: OrgRepeater,
            val isDelayUsed: Boolean,
            val delay: OrgDelay) {

        companion object {
            fun getInstance(str: String?): DateTime {
                val orgDateTime = OrgDateTime.parseOrNull(str)

                // TODO: Make default configurable
                val defaultTime = Calendar.getInstance().apply {
                    add(Calendar.HOUR, 1)
                }

                val cal = if (orgDateTime != null) {
                    orgDateTime.calendar
                } else {
                    defaultTime
                }

                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)

                // If there is no time part, use default
                val isTimeUsed = orgDateTime?.hasTime() ?: false
                val hour: Int
                val minute: Int
                if (orgDateTime != null && !orgDateTime.hasTime()) {
                    hour = defaultTime.get(Calendar.HOUR_OF_DAY)
                    minute = defaultTime.get(Calendar.MINUTE)
                } else {
                    hour = cal.get(Calendar.HOUR_OF_DAY)
                    minute = cal.get(Calendar.MINUTE)
                }

                val isEndTimeUsed = orgDateTime?.hasEndTime() ?: false
                val endHour = orgDateTime?.endCalendar?.get(Calendar.HOUR_OF_DAY)
                        ?: defaultTime.get(Calendar.HOUR_OF_DAY)
                val endMinute = orgDateTime?.endCalendar?.get(Calendar.MINUTE)
                        ?: defaultTime.get(Calendar.MINUTE)

                val isDelayUsed = orgDateTime?.hasDelay() ?: false
                val delay = orgDateTime?.delay ?: DEFAULT_DELAY

                val isRepeaterUsed = orgDateTime?.hasRepeater() ?: false
                val repeater = orgDateTime?.repeater ?: DEFAULT_REPEATER

                return DateTime(
                        year,
                        month,
                        day,
                        isTimeUsed,
                        hour,
                        minute,
                        isEndTimeUsed,
                        endHour,
                        endMinute,
                        isRepeaterUsed,
                        repeater,
                        isDelayUsed,
                        delay
                )
            }
        }
    }

    private val dateTimeMutable = MutableLiveData<DateTime>(DateTime.getInstance(orgDateTime))


    val dateTime: LiveData<DateTime>
        get() = dateTimeMutable


    fun getYearMonthDay(): Triple<Int, Int, Int> {
        val value = dateTime.value

        return if (value != null) {
            Triple(value.year, value.month, value.day)
        } else {
            defaultDate()
        }
    }

    fun getTimeHourMinute(): Pair<Int, Int> {
        val value = dateTime.value

        return if (value != null) {
            Pair(value.hour, value.minute)
        } else {
            defaultTime()
        }
    }

    fun getEndTimeHourMinute(): Pair<Int, Int> {
        val value = dateTime.value

        return if (value != null) {
            Pair(value.endHour, value.endMinute)
        } else {
            defaultTime()
        }
    }

    fun getRepeaterString(): String {
        return (dateTime.value?.repeater ?: DEFAULT_REPEATER).toString()
    }

    fun getDelayString(): String {
        return (dateTime.value?.delay ?: DEFAULT_DELAY).toString()
    }

    fun getOrgDateTime(): OrgDateTime? {
        return dateTime.value?.let {
            getOrgDateTime(it)
        }
    }

    fun getOrgDateTime(dateTime: DateTime): OrgDateTime {
        val builder = OrgDateTime.Builder()
                .setIsActive(true) // TODO: Add a checkbox or switch for this

                .setYear(dateTime.year)
                .setMonth(dateTime.month)
                .setDay(dateTime.day)

                .setHasTime(dateTime.isTimeUsed)
                .setHour(dateTime.hour)
                .setMinute(dateTime.minute)

                .setHasEndTime(dateTime.isEndTimeUsed)
                .setEndHour(dateTime.endHour)
                .setEndMinute(dateTime.endMinute)

                .setRepeater(if (dateTime.isRepeaterUsed) dateTime.repeater else null)
                .setDelay(if (dateTime.isDelayUsed) dateTime.delay else null)

        return builder.build()
    }

    fun set(year: Int, month: Int, day: Int) {
        val value = dateTime.value ?: DateTime.getInstance(null)

        val newValue = value.copy(year = year, month = month, day = day)

        dateTimeMutable.postValue(newValue)
    }
    
    fun setTime(hour: Int, minute: Int) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isTimeUsed = true, hour = hour, minute = minute))
        }
    }

    fun setEndTime(hour: Int, minute: Int) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isEndTimeUsed = true, endHour= hour, endMinute = minute))
        }
    }
    
    fun set(repeater: OrgRepeater) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isRepeaterUsed = true, repeater = repeater))
        }
    }

    fun set(delay: OrgDelay) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isDelayUsed = true, delay = delay))
        }
    }

    fun setIsTimeUsed(isChecked: Boolean) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isTimeUsed = isChecked))
        }
    }

    fun setIsEndTimeUsed(isChecked: Boolean) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isEndTimeUsed = isChecked))
        }
    }

    fun setIsRepeaterUsed(isChecked: Boolean) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isRepeaterUsed = isChecked))
        }
    }

    fun setIsDelayUsed(isChecked: Boolean) {
        dateTime.value?.let { value ->
            dateTimeMutable.postValue(value.copy(isDelayUsed = isChecked))
        }
    }

    companion object {
        private val TAG = TimestampDialogViewModel::class.java.name

        private val DEFAULT_REPEATER = OrgRepeater(
                OrgRepeater.Type.RESTART, 1, OrgInterval.Unit.WEEK)

        private val DEFAULT_DELAY = OrgDelay(
                OrgDelay.Type.ALL, 1, OrgInterval.Unit.DAY)

        private fun defaultDate() = Calendar.getInstance().let { cal ->
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }

        private fun defaultTime() = Calendar.getInstance().let { cal ->
            Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }
    }
}