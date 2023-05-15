package com.orgzly.android.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded


data class NoteView(
        @Embedded
        val note: Note,

        @ColumnInfo(name = "inherited_tags")
        val inheritedTags: String? = null,

        @ColumnInfo(name = "scheduled_range_string")
        val scheduledRangeString : String? = null,
        @ColumnInfo(name = "scheduled_time_string")
        val scheduledTimeString : String? = null,
        @ColumnInfo(name = "scheduled_time_end_string")
        val scheduledTimeEndString : String? = null,
        @ColumnInfo(name = "scheduled_time_timestamp")
        val scheduledTimeTimestamp : Long? = null,
        @ColumnInfo(name = "scheduled_time_start_of_day")
        val scheduledTimeStartOfDay : Long? = null,
        @ColumnInfo(name = "scheduled_time_hour")
        val scheduledTimeHour : Int? = null,

        @ColumnInfo(name = "deadline_range_string")
        val deadlineRangeString : String? = null,
        @ColumnInfo(name = "deadline_time_string")
        val deadlineTimeString : String? = null,
        @ColumnInfo(name = "deadline_time_end_string")
        val deadlineTimeEndString : String? = null,
        @ColumnInfo(name = "deadline_time_timestamp")
        val deadlineTimeTimestamp : Long? = null,
        @ColumnInfo(name = "deadline_time_start_of_day")
        val deadlineTimeStartOfDay : Long? = null,
        @ColumnInfo(name = "deadline_time_hour")
        val deadlineTimeHour : Int? = null,

        @ColumnInfo(name = "closed_range_string")
        val closedRangeString : String? = null,
        @ColumnInfo(name = "closed_time_string")
        val closedTimeString : String? = null,
        @ColumnInfo(name = "closed_time_end_string")
        val closedTimeEndString : String? = null,
        @ColumnInfo(name = "closed_time_timestamp")
        val closedTimeTimestamp : Long? = null,
        @ColumnInfo(name = "closed_time_start_of_day")
        val closedTimeStartOfDay : Long? = null,
        @ColumnInfo(name = "closed_time_hour")
        val closedTimeHour : Int? = null,

        @ColumnInfo(name = "clock_range_string")
        val clockRangeString : String? = null,
        @ColumnInfo(name = "clock_time_string")
        val clockTimeString : String? = null,
        @ColumnInfo(name = "clock_time_end_string")
        val clockTimeEndString : String? = null,

        @ColumnInfo(name = "event_string")
        val eventString : String? = null,
        @ColumnInfo(name = "event_timestamp")
        val eventTimestamp: Long? = null,
        @ColumnInfo(name = "event_end_timestamp")
        val eventEndTimestamp: Long? = null,
        @ColumnInfo(name = "event_start_of_day")
        val eventStartOfDay : Long? = null,
        @ColumnInfo(name = "event_hour")
        val eventHour : Int? = null,

        @ColumnInfo(name = "book_name")
        val bookName: String
) {
    fun hasInheritedTags(): Boolean {
        return inheritedTags != null
    }

    fun getInheritedTagsList(): List<String> {
        return Note.dbDeSerializeTags(inheritedTags)
    }
}
