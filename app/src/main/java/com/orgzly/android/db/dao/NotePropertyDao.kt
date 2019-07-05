package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.orgzly.android.db.entity.NoteProperty

@Dao
abstract class NotePropertyDao : BaseDao<NoteProperty> {

    @Query("SELECT * FROM note_properties WHERE note_id = :noteId ORDER BY position")
    abstract fun get(noteId: Long): List<NoteProperty>

    @Query("SELECT * FROM note_properties WHERE note_id = :noteId AND name = :name ORDER BY position")
    abstract fun get(noteId: Long, name: String): List<NoteProperty>

    @Query("SELECT * FROM note_properties")
    abstract fun getAll(): List<NoteProperty>

    @Transaction
    open fun upsert(noteId: Long, name: String, value: String) {
        val properties = get(noteId, name)

        if (properties.isEmpty()) {
            // Insert new
            val position = getNextAvailablePosition(noteId)
            insert(NoteProperty(noteId, position, name, value))

        } else {
            // Update first
            update(properties.first().copy(value = value))

            // Delete others
            for (i in 1 until properties.size) {
                delete(properties[i])
            }
        }
    }

    private fun getNextAvailablePosition(noteId: Long): Int {
        return getLastPosition(noteId).let {
            if (it != null) it + 1 else 1
        }
    }

    @Query("SELECT MAX(position) FROM note_properties WHERE note_id = :noteId")
    abstract fun getLastPosition(noteId: Long): Int?

    @Query("DELETE FROM note_properties WHERE note_id = :noteId")
    abstract fun delete(noteId: Long)
}
