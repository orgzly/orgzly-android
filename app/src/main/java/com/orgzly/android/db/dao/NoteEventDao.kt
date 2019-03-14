package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.NoteEvent

@Dao
abstract class NoteEventDao : BaseDao<NoteEvent> {
    @Query("SELECT * FROM note_events WHERE note_id = :noteId")
    abstract fun get(noteId: Long): List<NoteEvent>

    @Query("DELETE FROM note_events WHERE note_id = :noteId")
    abstract fun deleteForNote(noteId: Long)
}
