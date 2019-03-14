package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReminderTimeDao {
    data class NoteTime(
            var noteId: Long,
            var bookId: Long,
            var bookName: String,
            var state: String?,
            var title: String,
            var timeType: Int,
            var orgTimestampString: String)

    @Query("""
        SELECT
        n.id as noteId,
        n.book_id as bookId,
        coalesce(b.title, b.name) as bookName,
        n.state as state,
        n.title as title,
        $SCHEDULED_TIME as timeType,
        t.string as orgTimestampString
        FROM org_ranges r
        JOIN org_timestamps t ON (r.start_timestamp_id = t.id )
        JOIN notes n ON (r.id = n.scheduled_range_id)
        JOIN books b ON (b.id = n.book_id)
        WHERE t.is_active = 1

        UNION

        SELECT
        n.id as noteId,
        n.book_id as bookId,
        coalesce(b.title, b.name) as bookName,
        n.state as state,
        n.title as title,
        $DEADLINE_TIME as timeType,
        t.string as orgTimestampString
        FROM org_ranges r
        JOIN org_timestamps t ON (r.start_timestamp_id = t.id )
        JOIN notes n ON (r.id = n.deadline_range_id)
        JOIN books b ON (b.id = n.book_id)
        WHERE t.is_active = 1

        UNION

        SELECT
        n.id as noteId,
        n.book_id as bookId,
        coalesce(b.title, b.name) as bookName,
        n.state as state,
        n.title as title,
        $EVENT_TIME as timeType,
        t.string as orgTimestampString
        FROM note_events e
        JOIN org_ranges r ON (r.id = e.org_range_id)
        JOIN org_timestamps t ON (t.id = r.start_timestamp_id)
        JOIN notes n ON (n.id = e.note_id)
        JOIN books b ON (b.id = n.book_id)

    """)
    fun getAll(): List<NoteTime>

    companion object {
        const val SCHEDULED_TIME = 1
        const val DEADLINE_TIME = 2
        const val EVENT_TIME = 3
    }
}
