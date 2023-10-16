package com.orgzly.android.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NoteView
import org.intellij.lang.annotations.Language

@Dao
abstract class NoteViewDao {
    @Query("$QUERY WHERE notes.level > 0 GROUP BY notes.id")
    abstract fun getAll(): List<NoteView>

    @Query("""
        $QUERY
        WHERE notes.book_id = :bookId
        AND notes.level > 0
        AND notes.is_cut = 0
        AND notes.folded_under_id = 0
        GROUP BY notes.id
        ORDER BY notes.lft
    """)
    abstract fun getVisibleLiveData(bookId: Long): LiveData<List<NoteView>>

    @Query("""
        $QUERY
        WHERE notes.book_id = :bookId
        AND notes.level > 0
        AND notes.is_cut = 0
        AND notes.folded_under_id = 0
        AND :lft <= notes.lft
        AND notes.rgt <= :rgt
        GROUP BY notes.id
        ORDER BY notes.lft
    """)
    abstract fun getVisibleLiveData(bookId: Long, lft: Long, rgt: Long): LiveData<List<NoteView>>

    @RawQuery(observedEntities = [ Note::class, Book::class ])
    abstract fun runQueryLiveData(query: SupportSQLiteQuery): LiveData<List<NoteView>>

    @RawQuery(observedEntities = [ Note::class, Book::class ])
    abstract fun runQuery(query: SupportSQLiteQuery): List<NoteView>

    @Query("$QUERY WHERE notes.id = :id GROUP BY notes.id")
    abstract fun get(id: Long): NoteView?

    @Query("$QUERY WHERE notes.title = :title GROUP BY notes.id ORDER BY lft DESC LIMIT 1")
    abstract fun getLast(title: String): NoteView?

    @Query("$QUERY WHERE book_name = :bookName AND notes.level > 0 AND notes.is_cut = 0 GROUP BY notes.id ORDER BY notes.lft")
    abstract fun getBookNotes(bookName: String): List<NoteView>


    companion object {
        @Language("RoomSql")
        const val QUERY = """
            SELECT

            notes.*,

            group_concat(t_notes_with_inherited_tags.tags, ' ') AS inherited_tags,

            t_scheduled_range.string AS scheduled_range_string,
            t_scheduled_timestamps_start.string AS scheduled_time_string,
            t_scheduled_timestamps_end.string AS scheduled_time_end_string,
            t_scheduled_timestamps_start.timestamp AS scheduled_time_timestamp,
            datetime(t_scheduled_timestamps_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS scheduled_time_start_of_day,
            t_scheduled_timestamps_start.hour AS scheduled_time_hour,

            t_deadline_range.string AS deadline_range_string,
            t_deadline_timestamps_start.string AS deadline_time_string,
            t_deadline_timestamps_end.string AS deadline_time_end_string,
            t_deadline_timestamps_start.timestamp AS deadline_time_timestamp,
            datetime(t_deadline_timestamps_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS deadline_time_start_of_day,
            t_deadline_timestamps_start.hour AS deadline_time_hour,

            t_closed_range.string AS closed_range_string,
            t_closed_timestamps_start.string AS closed_time_string,
            t_closed_timestamps_end.string AS closed_time_end_string,
            t_closed_timestamps_start.string AS closed_time_timestamp,
            datetime(t_closed_timestamps_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS closed_time_start_of_day,
            t_closed_timestamps_start.hour AS closed_time_hour,

            t_clock_range.string AS clock_range_string,
            t_clock_timestamps_start.string AS clock_time_string,
            t_clock_timestamps_end.string AS clock_time_end_string,

            NULL AS event_string,
            NULL AS event_timestamp,
            NULL AS event_end_timestamp,
            NULL AS event_start_of_day,
            NULL AS event_hour,

            t_books.name AS book_name

            FROM notes

            LEFT JOIN org_ranges t_scheduled_range ON t_scheduled_range.id = notes.scheduled_range_id
            LEFT JOIN org_timestamps t_scheduled_timestamps_start ON t_scheduled_timestamps_start.id = t_scheduled_range.start_timestamp_id
            LEFT JOIN org_timestamps t_scheduled_timestamps_end ON t_scheduled_timestamps_end.id = t_scheduled_range.end_timestamp_id
            LEFT JOIN org_ranges t_deadline_range ON t_deadline_range.id = notes.deadline_range_id
            LEFT JOIN org_timestamps t_deadline_timestamps_start ON t_deadline_timestamps_start.id = t_deadline_range.start_timestamp_id
            LEFT JOIN org_timestamps t_deadline_timestamps_end ON t_deadline_timestamps_end.id = t_deadline_range.end_timestamp_id
            LEFT JOIN org_ranges t_closed_range ON t_closed_range.id = notes.closed_range_id
            LEFT JOIN org_timestamps t_closed_timestamps_start ON t_closed_timestamps_start.id = t_closed_range.start_timestamp_id
            LEFT JOIN org_timestamps t_closed_timestamps_end ON t_closed_timestamps_end.id = t_closed_range.end_timestamp_id
            LEFT JOIN org_ranges t_clock_range ON t_clock_range.id = notes.clock_range_id
            LEFT JOIN org_timestamps t_clock_timestamps_start ON t_clock_timestamps_start.id = t_clock_range.start_timestamp_id
            LEFT JOIN org_timestamps t_clock_timestamps_end ON t_clock_timestamps_end.id = t_clock_range.end_timestamp_id
            LEFT JOIN books t_books ON t_books.id = notes.book_id
            LEFT JOIN note_ancestors t_note_ancestors ON t_note_ancestors.note_id = notes.id
            LEFT JOIN notes t_notes_with_inherited_tags ON t_notes_with_inherited_tags.id = t_note_ancestors.ancestor_note_id
        """

        @Language("RoomSql")
        const val QUERY_WITH_NOTE_EVENTS = """
            SELECT

            notes.*,

            group_concat(t_notes_with_inherited_tags.tags, ' ') AS inherited_tags,

            t_scheduled_range.string AS scheduled_range_string,
            t_scheduled_timestamps_start.is_active AS scheduled_is_active,
            t_scheduled_timestamps_start.string AS scheduled_time_string,
            t_scheduled_timestamps_end.string AS scheduled_time_end_string,
            t_scheduled_timestamps_start.timestamp AS scheduled_time_timestamp,
            datetime(t_scheduled_timestamps_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS scheduled_time_start_of_day,
            t_scheduled_timestamps_start.hour AS scheduled_time_hour,

            t_deadline_range.string AS deadline_range_string,
            t_deadline_timestamps_start.is_active AS deadline_is_active,
            t_deadline_timestamps_start.string AS deadline_time_string,
            t_deadline_timestamps_end.string AS deadline_time_end_string,
            t_deadline_timestamps_start.timestamp AS deadline_time_timestamp,
            datetime(t_deadline_timestamps_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS deadline_time_start_of_day,
            t_deadline_timestamps_start.hour AS deadline_time_hour,

            t_closed_range.string AS closed_range_string,
            t_closed_timestamps_start.string AS closed_time_string,
            t_closed_timestamps_end.string AS closed_time_end_string,
            t_closed_timestamps_start.timestamp AS closed_time_timestamp,
            datetime(t_closed_timestamps_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS closed_time_start_of_day,
            t_closed_timestamps_start.hour AS closed_time_hour,

            t_clock_range.string AS clock_range_string,
            t_clock_timestamps_start.string AS clock_time_string,
            t_clock_timestamps_end.string AS clock_time_end_string,

            t_note_events_range.string AS event_string,
            t_note_events_start.timestamp AS event_timestamp,
            COALESCE(t_note_events_start.end_timestamp, t_note_events_end.timestamp, t_note_events_start.timestamp) AS event_end_timestamp,
            datetime(t_note_events_start.timestamp/1000, 'unixepoch', 'localtime', 'start of day') AS event_start_of_day,
            t_note_events_start.hour AS event_hour,

            t_books.name AS book_name

            FROM notes

            LEFT JOIN org_ranges t_scheduled_range ON t_scheduled_range.id = notes.scheduled_range_id
            LEFT JOIN org_timestamps t_scheduled_timestamps_start ON t_scheduled_timestamps_start.id = t_scheduled_range.start_timestamp_id
            LEFT JOIN org_timestamps t_scheduled_timestamps_end ON t_scheduled_timestamps_end.id = t_scheduled_range.end_timestamp_id
            LEFT JOIN org_ranges t_deadline_range ON t_deadline_range.id = notes.deadline_range_id
            LEFT JOIN org_timestamps t_deadline_timestamps_start ON t_deadline_timestamps_start.id = t_deadline_range.start_timestamp_id
            LEFT JOIN org_timestamps t_deadline_timestamps_end ON t_deadline_timestamps_end.id = t_deadline_range.end_timestamp_id
            LEFT JOIN org_ranges t_closed_range ON t_closed_range.id = notes.closed_range_id
            LEFT JOIN org_timestamps t_closed_timestamps_start ON t_closed_timestamps_start.id = t_closed_range.start_timestamp_id
            LEFT JOIN org_timestamps t_closed_timestamps_end ON t_closed_timestamps_end.id = t_closed_range.end_timestamp_id
            LEFT JOIN org_ranges t_clock_range ON t_clock_range.id = notes.clock_range_id
            LEFT JOIN org_timestamps t_clock_timestamps_start ON t_clock_timestamps_start.id = t_clock_range.start_timestamp_id
            LEFT JOIN org_timestamps t_clock_timestamps_end ON t_clock_timestamps_end.id = t_clock_range.end_timestamp_id
            LEFT JOIN books t_books ON t_books.id = notes.book_id
            LEFT JOIN note_ancestors t_note_ancestors ON t_note_ancestors.note_id = notes.id
            LEFT JOIN notes t_notes_with_inherited_tags ON t_notes_with_inherited_tags.id = t_note_ancestors.ancestor_note_id

            LEFT JOIN note_events t_note_events ON t_note_events.note_id = notes.id
            LEFT JOIN org_ranges t_note_events_range ON t_note_events_range.id = t_note_events.org_range_id
            LEFT JOIN org_timestamps t_note_events_start ON t_note_events_start.id = t_note_events_range.start_timestamp_id
            LEFT JOIN org_timestamps t_note_events_end ON t_note_events_end.id = t_note_events_range.end_timestamp_id

            GROUP BY notes.id, event_timestamp
        """
    }
}
