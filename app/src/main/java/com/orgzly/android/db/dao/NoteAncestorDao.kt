package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.db.entity.NoteAncestor

@Dao
abstract class NoteAncestorDao(val db: OrgzlyDatabase) : BaseDao<NoteAncestor> {
    @Query("""
        DELETE FROM note_ancestors
        WHERE note_id IN (
            SELECT DISTINCT d.id
            FROM notes n, notes d
            WHERE d.book_id = n.book_id AND n.id IN (:ids) AND d.is_cut = 0 AND n.lft <= d.lft AND d.rgt <= n.rgt
        )
    """)
    abstract fun deleteForSubtrees(ids: Set<Long>)

    /*
     * "INSERT query type is not supported yet"
     * https://issuetracker.google.com/issues/109900809
     *
     * They are now, though inspection is still not passing
     * https://issuetracker.google.com/issues/109900809#comment9
     */


    @Transaction
    open fun insertAncestorsForNotes(ids: Set<Long>) {
        ids.chunked(OrgzlyDatabase.SQLITE_MAX_VARIABLE_NUMBER).forEach { chunk ->
            insertAncestorsForNotesChunk(chunk)
        }
    }

    @Query("""
        INSERT INTO note_ancestors (book_id, note_id, ancestor_note_id)
        SELECT n.book_id, n.id, a.id
        FROM notes n
        JOIN notes a ON (n.book_id = a.book_id AND a.lft < n.lft AND n.rgt < a.rgt)
        WHERE n.id IN (:ids)
    """)
    abstract fun insertAncestorsForNotesChunk(ids: List<Long>)

    @Query("""
        INSERT INTO note_ancestors (book_id, note_id, ancestor_note_id)
        SELECT n.book_id, n.id, a.id
        FROM notes n
        JOIN notes a ON (n.book_id = a.book_id AND a.lft < n.lft AND n.rgt < a.rgt)
        WHERE n.id = :noteId AND a.level > 0
    """)
    abstract fun insertAncestorsForNote(noteId: Long)
}
