package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.OrgzlyDatabase
import com.orgzly.android.db.entity.NoteAncestor

@Dao
abstract class NoteAncestorDao(val db: OrgzlyDatabase) : BaseDao<NoteAncestor> {
    @Query("""
        DELETE FROM note_ancestors
        WHERE note_id IN (
            SELECT DISTINCT d.id
            FROM notes n, notes d
            WHERE d.book_id = :bookId AND n.book_id = :bookId AND n.id IN (:ids) AND d.is_cut = 0 AND n.lft <= d.lft AND d.rgt <= n.rgt
        )
    """)
    abstract fun deleteForNoteAndDescendants(bookId: Long, ids: Set<Long>)

    /*
     * "INSERT query type is not supported yet"
     * https://issuetracker.google.com/issues/109900809
     */

    fun insertAncestorsForBatch(batchId: Long) {
        db.openHelper.writableDatabase.execSQL("""
                INSERT INTO note_ancestors (book_id, note_id, ancestor_note_id)
                SELECT n.book_id, n.id, a.id
                FROM notes n
                JOIN notes a ON (n.book_id = a.book_id AND a.lft < n.lft AND n.rgt < a.rgt)
                WHERE n.is_cut = $batchId AND a.level > 0
            """)
    }

    fun insertAncestorsForNote(noteId: Long) {
        db.openHelper.writableDatabase.execSQL("""
                INSERT INTO note_ancestors (book_id, note_id, ancestor_note_id)
                SELECT n.book_id, n.id, a.id
                FROM notes n
                JOIN notes a ON (n.book_id = a.book_id AND a.lft < n.lft AND n.rgt < a.rgt)
                WHERE n.id = $noteId AND a.level > 0
            """)

    }
}
