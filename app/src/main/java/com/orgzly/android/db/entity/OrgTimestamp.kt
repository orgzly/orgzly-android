package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "org_timestamps",

        indices = [
            Index("string", unique = true),
            Index("timestamp"),
            Index("end_timestamp")
        ]
)
data class OrgTimestamp(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        val string: String,

        @ColumnInfo(name = "is_active")
        val isActive: Boolean,

        val year: Int,

        val month: Int,

        val day: Int,

        val hour: Int?,

        val minute: Int?,

        val second: Int?,

        @ColumnInfo(name = "end_hour")
        val endHour: Int?,

        @ColumnInfo(name = "end_minute")
        val endMinute: Int?,

        @ColumnInfo(name = "end_second")
        val endSecond: Int?,

        @ColumnInfo(name = "repeater_type")
        val repeaterType: Int?,

        @ColumnInfo(name = "repeater_value")
        val repeaterValue: Int?,

        @ColumnInfo(name = "repeater_unit")
        val repeaterUnit: Int?,

        @ColumnInfo(name = "habit_deadline_value")
        val habitDeadlineValue: Int?,

        @ColumnInfo(name = "habit_deadline_unit")
        val habitDeadlineUnit: Int?,

        @ColumnInfo(name = "delay_type")
        val delayType: Int?,

        @ColumnInfo(name = "delay_value")
        val delayValue: Int?,

        @ColumnInfo(name = "delay_unit")
        val delayUnit: Int?,

        val timestamp: Long,

        @ColumnInfo(name = "end_timestamp")
        val endTimestamp: Long?
)