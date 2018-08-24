package com.orgzly.android.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded


data class NoteView(
        @Embedded
        val note: Note,

        @ColumnInfo(name = "inherited_tags")
        val inheritedTags: String?,

        @ColumnInfo(name = "scheduled_range_string")
        val scheduledRangeString : String?,
        @ColumnInfo(name = "scheduled_time_string")
        val scheduledTimeString : String?,
        @ColumnInfo(name = "scheduled_time_end_string")
        val scheduledTimeEndString : String?,
        @ColumnInfo(name = "scheduled_time_timestamp")
        val scheduledTimeTimestamp : Long?,
        @ColumnInfo(name = "scheduled_time_start_of_day")
        val scheduledTimeStartOfDay : Long?,
        @ColumnInfo(name = "scheduled_time_hour")
        val scheduledTimeHour : Int?,

        @ColumnInfo(name = "deadline_range_string")
        val deadlineRangeString : String?,
        @ColumnInfo(name = "deadline_time_string")
        val deadlineTimeString : String?,
        @ColumnInfo(name = "deadline_time_end_string")
        val deadlineTimeEndString : String?,
        @ColumnInfo(name = "deadline_time_timestamp")
        val deadlineTimeTimestamp : Long?,
        @ColumnInfo(name = "deadline_time_start_of_day")
        val deadlineTimeStartOfDay : Long?,
        @ColumnInfo(name = "deadline_time_hour")
        val deadlineTimeHour : Int?,

        @ColumnInfo(name = "closed_range_string")
        val closedRangeString : String?,
        @ColumnInfo(name = "closed_time_string")
        val closedTimeString : String?,
        @ColumnInfo(name = "closed_time_end_string")
        val closedTimeEndString : String?,
        @ColumnInfo(name = "closed_time_timestamp")
        val closedTimeTimestamp : Long?,
        @ColumnInfo(name = "closed_time_start_of_day")
        val closedTimeStartOfDay : Long?,
        @ColumnInfo(name = "closed_time_hour")
        val closedTimeHour : Int?,

        @ColumnInfo(name = "clock_range_string")
        val clockRangeString : String?,
        @ColumnInfo(name = "clock_time_string")
        val clockTimeString : String?,
        @ColumnInfo(name = "clock_time_end_string")
        val clockTimeEndString : String?,

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
