package com.orgzly.android.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NotePosition


@Dao
abstract class NoteDao : BaseDao<Note> {
    @Query("SELECT * FROM notes WHERE level > 0")
    abstract fun getAll(): List<Note>

    @Query("SELECT count(*) FROM notes WHERE book_id = :bookId AND level > 0 AND is_cut = 0")
    abstract fun getCount(bookId: Long): Int

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract fun get(id: Long): Note?

    @Query("SELECT * FROM notes WHERE title = :title ORDER BY lft DESC LIMIT 1")
    abstract fun get(title: String): Note?

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    abstract fun get(ids: Set<Long>): List<Note>

    @Query("SELECT DISTINCT tags FROM notes WHERE tags IS NOT NULL AND tags != ''")
    abstract fun getDistinctTagsLiveData(): LiveData<List<String>>

    @Query("SELECT DISTINCT tags FROM notes WHERE tags IS NOT NULL AND tags != ''")
    abstract fun getDistinctTags(): List<String>

    @Query("""
        DELETE FROM notes
        WHERE id IN (
        SELECT DISTINCT d.id
        FROM notes n, notes d
        WHERE n.id IN (:ids) AND d.book_id = n.book_id AND d.is_cut = 0 AND n.lft <= d.lft AND d.rgt <= n.rgt
        )
    """)
    abstract fun deleteById(ids: Set<Long>): Int

    @Query("DELETE FROM notes WHERE book_id = :bookId")
    abstract fun deleteByBookId(bookId: Long)

    /**
     * Marks note and all its descendants as cut.
     */
    @Query("""
        UPDATE notes
        SET is_cut = :batchId
        WHERE id IN (
        SELECT DISTINCT d.id
        FROM notes n, notes d
        WHERE d.book_id = :bookId AND n.book_id = :bookId AND n.id IN (:ids) AND d.is_cut = 0 AND n.is_cut = 0 AND n.lft <= d.lft AND d.rgt <= n.rgt
        )
    """)
    abstract fun markAsCut(bookId: Long, ids: Set<Long>, batchId: Long): Int

    @Query(SELECT_ANCESTORS_IDS)
    abstract fun getAncestors(ids: List<Long>): List<Long>

    @Query("""
            SELECT a.*
            FROM notes n, notes a
            WHERE n.id = (:id)
            AND n.book_id = a.book_id
            AND a.is_cut = 0
            AND a.level > 0
            AND a.lft < n.lft
            AND n.rgt < a.rgt
            ORDER BY a.lft DESC

    """)
    abstract fun getAncestors(id: Long): List<Note>

    @Query("""
        SELECT count(*)
        FROM notes
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND (is_folded IS NULL OR is_folded = 0)
    """)
    abstract fun getBookUnfoldedNoteCount(bookId: Long): Int


    @Query("""
        SELECT * FROM notes
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND rgt < :lft AND parent_id = :parentId
        ORDER BY lft DESC
        LIMIT 1
    """)
    abstract fun getPreviousSibling(bookId: Long, lft: Long, parentId: Long): Note?

    @Query("""
        SELECT * FROM notes
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND :rgt < lft AND parent_id = :parentId
        ORDER BY lft
        LIMIT 1
    """)
    abstract fun getNextSibling(bookId: Long, rgt: Long, parentId: Long): Note?

    @Query("""
        SELECT *
        FROM notes
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND :lft < lft AND rgt < :rgt
        ORDER BY lft DESC
        LIMIT 1
    """)
    abstract fun getFirstDescendant(bookId: Long, lft: Long, rgt: Long): Note?

    @Query("""
        SELECT *
        FROM notes
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND :lft < lft AND rgt < :rgt
        ORDER BY level, lft DESC
        LIMIT 1
    """)
    abstract fun getLastHighestLevelDescendant(bookId: Long, lft: Long, rgt: Long): Note?

    @Query("""
        UPDATE notes
        SET descendants_count = (
            SELECT count(*)
            FROM notes d
            WHERE (notes.book_id = d.book_id AND $WHERE_EXISTING_NOTES AND notes.lft < d.lft AND d.rgt < notes.rgt)
        )
        WHERE id IN ($SELECT_ANCESTORS_IDS)
    """)
    abstract fun updateDescendantsCountForAncestors(ids: Set<Long>)


    @Query("""
        UPDATE notes
        SET descendants_count = descendants_count + 1
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND lft < :lft AND :rgt < rgt
    """)
    abstract fun incrementDescendantsCountForAncestors(bookId: Long, lft: Long, rgt: Long): Int

    @Query("UPDATE notes SET rgt = rgt + 2 WHERE book_id = :bookId AND level = 0")
    abstract fun incrementRgtForRootNote(bookId: Long)

    @Query("SELECT id FROM notes WHERE book_id = :bookId AND level = 0")
    abstract fun getRootNodeId(bookId: Long): Long?

    @Query("SELECT * FROM notes WHERE book_id = :bookId AND level = 0")
    abstract fun getRootNode(bookId: Long): Note?

    @Query("""
        UPDATE notes
        SET descendants_count = (
            SELECT count(*)
            FROM notes d
            WHERE (notes.book_id = d.book_id AND $WHERE_EXISTING_NOTES AND notes.lft < d.lft AND d.rgt < notes.rgt)
        )
        WHERE id IN ($SELECT_NOTE_AND_ANCESTORS_IDS)
    """)
    abstract fun updateDescendantsCountForNoteAndAncestors(ids: List<Long>)

    @Query("UPDATE notes SET content = :content WHERE id = :id")
    abstract fun updateContent(id: Long, content: String?)

    @Query("""
        UPDATE notes
        SET state = :state, scheduled_range_id = :scheduled, deadline_range_id = :deadline, closed_range_id = :closed
        WHERE id = :id
    """)
    abstract fun update(id: Long, state: String?, scheduled: Long?, deadline: Long?, closed: Long?): Int

    @Query("UPDATE notes SET title = :title, state = :state, priority = :priority WHERE id = :id")
    abstract fun update(id: Long, title: String, state: String?, priority: String?): Int

    @Query("UPDATE notes SET scheduled_range_id = :timeId WHERE id IN (:ids)")
    abstract fun updateScheduledTime(ids: Set<Long>, timeId: Long?)

    @Query("UPDATE notes SET deadline_range_id = :timeId WHERE id IN (:ids)")
    abstract fun updateDeadlineTime(ids: Set<Long>, timeId: Long?)


    @Transaction
    open fun foldAll(bookId: Long) {
        foldAll1(bookId)
        foldAll2(bookId)
    }

    @Query("""
        UPDATE notes
        SET folded_under_id = parent_id
        WHERE (book_id = :bookId AND $WHERE_EXISTING_NOTES) AND level > (
            SELECT min(level) FROM notes WHERE (book_id = :bookId AND $WHERE_EXISTING_NOTES)
        )
    """)
    abstract fun foldAll1(bookId: Long)

    @Query("UPDATE notes SET is_folded = 1 WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES")
    abstract fun foldAll2(bookId: Long)

    @Query("UPDATE notes SET is_folded = 0, folded_under_id = 0 WHERE book_id = :bookId")
    abstract fun unfoldAll(bookId: Long)

    @Query("UPDATE notes SET is_folded = 0 WHERE id IN (:ids)")
    abstract fun unfoldNotes(ids: List<Long>)

    @Query("UPDATE notes SET folded_under_id = 0 WHERE folded_under_id IN (:ids)")
    abstract fun updateFoldedUnderForNoteFoldedUnderId(ids: List<Long>)

    @Query("UPDATE notes SET is_folded = :isFolded WHERE id = :id")
    abstract fun updateIsFolded(id: Long, isFolded: Boolean): Int

    /** All descendants which are not already hidden (due to one of the ancestors being folded). */
    @Query("""
        UPDATE notes
        SET folded_under_id = :noteId
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND :lft < lft AND rgt < :rgt
            AND (folded_under_id IS NULL OR folded_under_id = 0)
    """)
    abstract fun foldDescendantsUnderId(bookId: Long, noteId: Long, lft: Long, rgt: Long)

    /** All descendants which are hidden because of this note being folded. */
    @Query("""
        UPDATE notes
        SET folded_under_id = 0
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND :lft < lft AND rgt < :rgt
            AND folded_under_id = :noteId
    """)
    abstract fun unfoldDescendantsUnderId(bookId: Long, noteId: Long, lft: Long, rgt: Long)

    @Query("SELECT min(lft) as minLft, max(rgt) as maxRgt, min(level) as minLevel FROM notes WHERE is_cut = :batchId")
    abstract fun getBatchData(batchId: Long): Batch?

    @Query("UPDATE notes SET lft = lft + :inc WHERE (book_id = :bookId AND $WHERE_EXISTING_NOTES) AND lft >= :value")
    abstract fun incrementLftForLftGe(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET lft = lft + :inc WHERE (book_id = :bookId AND $WHERE_EXISTING_NOTES) AND lft > :value")
    abstract fun incrementLftForLftGt(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET rgt = rgt + :inc WHERE book_id = :bookId AND is_cut = 0 AND rgt > :value")
    abstract fun incrementRgtForRgtGtOrRoot(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET rgt = rgt + :inc WHERE book_id = :bookId AND is_cut = 0 AND rgt >= :value")
    abstract fun incrementRgtForRgtGeOrRoot(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET folded_under_id = 0 WHERE is_cut = :batchId AND folded_under_id NOT IN (SELECT id FROM notes WHERE is_cut = :batchId)")
    abstract fun unfoldNotBelongingToBatch(batchId: Long)

    @Query("UPDATE notes SET folded_under_id = :foldedUnder WHERE is_cut = :batchId AND folded_under_id = 0")
    abstract fun markBatchAsFolded(batchId: Long, foldedUnder: Long)

    @Query("UPDATE notes SET parent_id = :parentId WHERE id = :noteId")
    abstract fun updateParentForNote(noteId: Long, parentId: Long)

    @Query("UPDATE notes SET parent_id = :parentId WHERE is_cut = :batchId AND lft = :batchMinLft")
    abstract fun updateParentOfBatchRoot(batchId: Long, batchMinLft: Long, parentId: Long)

    @Query("""
        UPDATE notes
        SET lft = lft + :positionOffset, rgt = rgt + :positionOffset, level = level + :levelOffset, book_id = :bookId
        WHERE is_cut = :batchId
    """)
    abstract fun moveBatch(batchId: Long, bookId: Long, positionOffset: Long, levelOffset: Int)

    @Query("UPDATE notes SET is_cut = 0 WHERE is_cut = :batchId")
    abstract fun makeBatchVisible(batchId: Long): Int

    @Query("DELETE FROM notes WHERE is_cut != 0")
    abstract fun deleteCut()

    @Query("SELECT MAX(is_cut) AS batch_id FROM notes WHERE is_cut IS NOT NULL AND is_cut != 0")
    abstract fun getLatestBatchId(): Long?

    @Query("""
        SELECT notes.id as noteId, notes.book_id as bookId
        FROM note_properties
        LEFT JOIN notes ON (notes.id = note_properties.note_id)
        WHERE LOWER(note_properties.name) = :name AND LOWER(note_properties.value) = :value AND notes.id IS NOT NULL
        ORDER BY notes.lft
        LIMIT 1
    """)
    abstract fun firstNoteHavingPropertyLowerCase(name: String, value: String): NoteIdBookId?

    @Query("""
        UPDATE notes
        SET state = :state, closed_range_id = null
        WHERE id IN (:ids) AND COALESCE(state, "") != COALESCE(:state, "")
    """)
    abstract fun updateStateAndRemoveClosedTime(ids: Set<Long>, state: String?): Int

    @Query("""
        SELECT notes.id as noteId, state, content, st.string as scheduled, dt.string as deadline

        FROM notes

        LEFT JOIN org_ranges sr ON (sr.id = notes.scheduled_range_id)
        LEFT JOIN org_timestamps st ON (st.id = sr.start_timestamp_id)

        LEFT JOIN org_ranges dr ON (dr.id = notes.deadline_range_id)
        LEFT JOIN org_timestamps dt ON (dt.id = dr.start_timestamp_id)

        WHERE notes.id IN (:ids) AND COALESCE(state, "") != COALESCE(:state, "")
    """)
    abstract fun getStateUpdatedNotes(ids: Set<Long>, state: String?): List<NoteForStateUpdate>

    @Query("""SELECT DISTINCT book_id FROM notes WHERE id IN (:ids) AND COALESCE(state, "") != COALESCE(:state, "")""")
    abstract fun getBookIdsForNotesNotMatchingState(ids: Set<Long>, state: String?): List<Long>

    @Query("SELECT MAX(rgt) FROM notes WHERE book_id = :bookId AND is_cut = 0")
    abstract fun getMaxRgtForBook(bookId: Long): Long?

    @Query("UPDATE notes SET created_at= :time WHERE id = :noteId")
    abstract fun updateCreatedAtTime(noteId: Long, time: Long)

    companion object {
        /* Every book has a root note with level 0. */
        const val WHERE_EXISTING_NOTES = "(is_cut = 0 AND level > 0)"

        const val SELECT_ANCESTORS_IDS = """
            SELECT DISTINCT a.id
            FROM notes n, notes a
            WHERE n.id IN (:ids)
            AND n.book_id = a.book_id
            AND a.is_cut = 0
            AND a.level > 0
            AND a.lft < n.lft
            AND n.rgt < a.rgt
            """

        const val SELECT_NOTE_AND_ANCESTORS_IDS = """
            SELECT DISTINCT a.id FROM notes n, notes a
            WHERE n.id IN (:ids) AND n.book_id = a.book_id AND a.is_cut = 0 AND a.level > 0 AND a.lft <= n.lft AND n.rgt <= a.rgt """

        fun rootNote(bookId: Long): Note {
            return Note(id = 0, position = NotePosition(bookId, lft = 1, rgt = 2, level = 0))
        }
    }

    data class Batch(val minLft: Long, val maxRgt: Long, val minLevel: Int)

    data class NoteIdBookId(val noteId: Long, val bookId: Long)

    data class NoteForStateUpdate(
            val noteId: Long,
            val state: String?,
            val content: String?,
            val scheduled: String?,
            val deadline: String?)
}
