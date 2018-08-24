package com.orgzly.android.db.mappers

import com.orgzly.android.db.entity.OrgTimestamp
import com.orgzly.org.datetime.OrgDateTime
import com.orgzly.org.datetime.OrgDelay
import com.orgzly.org.datetime.OrgInterval
import com.orgzly.org.datetime.OrgRepeater
import java.util.*

/**
 * <2017-04-16 Sun>
 * <2017-01-02 Mon 13:00>
 * <2017-04-16 Sun .+1d>
 * <2017-01-02 Mon 09:00 ++1d/2d>
 * <2017-04-16 Sun .+1d -0d>
 * <2006-11-02 Thu 20:00-22:00>
 */
object OrgTimestampMapper {
    fun fromOrgDateTime(dt: OrgDateTime): OrgTimestamp {

        val string = dt.toString()

        val isActive = dt.isActive

        val year = dt.calendar.get(Calendar.YEAR)
        val month = dt.calendar.get(Calendar.MONTH) + 1
        val day = dt.calendar.get(Calendar.DAY_OF_MONTH)

        val hour = if (dt.hasTime()) dt.calendar.get(Calendar.HOUR_OF_DAY) else null
        val minute = if (dt.hasTime()) dt.calendar.get(Calendar.MINUTE) else null
        val second = if (dt.hasTime()) dt.calendar.get(Calendar.SECOND) else null

        val timestamp = dt.calendar.timeInMillis

        val endHour = if (dt.hasEndTime()) dt.endCalendar.get(Calendar.HOUR_OF_DAY) else null
        val endMinute = if (dt.hasEndTime()) dt.endCalendar.get(Calendar.MINUTE) else null
        val endSecond = if (dt.hasEndTime()) dt.endCalendar.get(Calendar.SECOND) else null

        val endTimestamp = if (dt.hasEndTime()) dt.endCalendar.timeInMillis else null

        var repeaterType: Int? = null
        var repeaterValue: Int? = null
        var repeaterUnit: Int? = null

        var habitDeadlineValue: Int? = null
        var habitDeadlineUnit: Int? = null

        if (dt.hasRepeater()) {
            repeaterType = repeaterType(dt.repeater.type)
            repeaterValue = dt.repeater.value
            repeaterUnit = timeUnit(dt.repeater.unit)

            if (dt.repeater.hasHabitDeadline()) {
                habitDeadlineValue = dt.repeater.habitDeadline.value
                habitDeadlineUnit = timeUnit(dt.repeater.habitDeadline.unit)
            }
        }

        var delayType: Int? = null
        var delayValue: Int? = null
        var delayUnit: Int? = null

        if (dt.hasDelay()) {
            delayType = delayType(dt.delay.type)
            delayValue = dt.delay.value
            delayUnit = timeUnit(dt.delay.unit)
        }
        
        return OrgTimestamp(
                0,
                string,
                isActive,
                year,
                month,
                day,
                hour,
                minute,
                second,
                endHour,
                endMinute,
                endSecond,
                repeaterType,
                repeaterValue,
                repeaterUnit,
                habitDeadlineValue,
                habitDeadlineUnit,
                delayType,
                delayValue,
                delayUnit,
                timestamp,
                endTimestamp
        )
    }

    private const val UNIT_HOUR = 360
    private const val UNIT_DAY = 424
    private const val UNIT_WEEK = 507
    private const val UNIT_MONTH = 604
    private const val UNIT_YEAR = 712

    private const val REPEATER_TYPE_CUMULATE = 20
    private const val REPEATER_TYPE_CATCH_UP = 22
    private const val REPEATER_TYPE_RESTART = 12

    private const val DELAY_TYPE_ALL = 1
    private const val DELAY_TYPE_FIRST_ONLY = 2

    fun repeaterType(type: OrgRepeater.Type): Int {
        return when (type) {
            OrgRepeater.Type.CUMULATE -> REPEATER_TYPE_CUMULATE
            OrgRepeater.Type.CATCH_UP -> REPEATER_TYPE_CATCH_UP
            OrgRepeater.Type.RESTART -> REPEATER_TYPE_RESTART
            else -> 0
        }
    }

    fun timeUnit(unit: OrgInterval.Unit): Int {
        return when (unit) {
            OrgInterval.Unit.HOUR -> UNIT_HOUR
            OrgInterval.Unit.DAY -> UNIT_DAY
            OrgInterval.Unit.WEEK -> UNIT_WEEK
            OrgInterval.Unit.MONTH -> UNIT_MONTH
            OrgInterval.Unit.YEAR -> UNIT_YEAR
            else -> 0
        }
    }

    fun delayType(type: OrgDelay.Type): Int {
        return when (type) {
            OrgDelay.Type.ALL -> DELAY_TYPE_ALL
            OrgDelay.Type.FIRST_ONLY -> DELAY_TYPE_FIRST_ONLY
            else -> 0
        }
    }
}