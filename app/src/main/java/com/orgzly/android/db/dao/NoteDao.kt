package com.orgzly.android.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.orgzly.android.db.entity.Note
import com.orgzly.android.db.entity.NotePosition
import org.intellij.lang.annotations.Language


@Dao
abstract class NoteDao : BaseDao<Note> {
    @Query("SELECT * FROM notes WHERE level > 0")
    abstract fun getAll(): List<Note>

    @Query("SELECT count(*) FROM notes WHERE book_id = :bookId AND level > 0 AND is_cut = 0")
    abstract fun getCount(bookId: Long): Int

    @Query("SELECT * FROM notes WHERE id = :id")
    abstract fun get(id: Long): Note?

    @Query("SELECT * FROM notes WHERE title = :title ORDER BY lft DESC LIMIT 1")
    abstract fun getLast(title: String): Note?

    @Query("SELECT * FROM notes WHERE id IN (:ids)")
    abstract fun get(ids: Set<Long>): List<Note>

    @Query("SELECT * FROM notes WHERE id IN (:ids) ORDER BY lft LIMIT 1")
    abstract fun getFirst(ids: Set<Long>): Note?

    @Query("SELECT * FROM notes WHERE id IN (:ids) ORDER BY lft DESC LIMIT 1")
    abstract fun getLast(ids: Set<Long>): Note?

    @Query("SELECT * FROM notes WHERE book_id = :bookId AND level = 1 AND $WHERE_EXISTING_NOTES ORDER BY lft")
    abstract fun getTopLevel(bookId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE parent_id = :id AND $WHERE_EXISTING_NOTES ORDER BY lft")
    abstract fun getChildren(id: Long): List<Note>

    @Query("SELECT DISTINCT tags FROM notes WHERE tags IS NOT NULL AND tags != ''")
    abstract fun getDistinctTagsLiveData(): LiveData<List<String>>

    @Query("SELECT DISTINCT tags FROM notes WHERE tags IS NOT NULL AND tags != ''")
    abstract fun getDistinctTags(): List<String>

    @Query("DELETE FROM notes WHERE id IN ($SELECT_SUBTREE_IDS_FOR_IDS)")
    abstract fun deleteById(ids: Set<Long>): Int

    @Query("DELETE FROM notes WHERE book_id = :bookId")
    abstract fun deleteByBookId(bookId: Long)

    @Query(SELECT_NOTE_AND_ANCESTORS_IDS_FOR_IDS)
    abstract fun getNoteAndAncestorsIds(ids: List<Long>): List<Long>

    @Query("""
            SELECT a.*
            FROM notes n, notes a
            WHERE n.id = (:id)
            AND n.book_id = a.book_id
            AND a.is_cut = 0
            AND a.level > 0
            AND a.lft < n.lft
            AND n.rgt < a.rgt
            ORDER BY a.lft
    """)
    abstract fun getAncestors(id: Long): List<Note>

    @Query("""
            SELECT a.*
            FROM notes n, notes a
            WHERE n.id = (:id)
            AND n.book_id = a.book_id
            AND a.is_cut = 0
            AND a.level > 0
            AND a.lft <= n.lft
            AND n.rgt <= a.rgt
            ORDER BY a.lft
    """)
    abstract fun getNoteAndAncestors(id: Long): List<Note>

    @Query(SELECT_SUBTREE_FOR_IDS)
    abstract fun getNotesForSubtrees(ids: Set<Long>): List<Note>

    @Query("SELECT count(*) FROM ($SELECT_SUBTREE_FOR_IDS)")
    abstract fun getNotesForSubtreesCount(ids: Set<Long>): Int

    @Query("""
        SELECT count(*)
        FROM notes
        WHERE book_id = :bookId AND $WHERE_EXISTING_NOTES AND (is_folded IS NULL OR is_folded = 0)
    """)
    abstract fun getBookUnfoldedNoteCount(bookId: Long): Int

    @Query("""
        SELECT count(*)
        FROM notes
        WHERE id IN ($SELECT_SUBTREE_IDS_FOR_IDS) AND is_folded = 1
    """)
    abstract fun getSubtreeFoldedNoteCount(ids: List<Long>): Int

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
        ORDER BY level, lft DESC
        LIMIT 1
    """)
    abstract fun getLastHighestLevelDescendant(bookId: Long, lft: Long, rgt: Long): Note?

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
        WHERE id IN ($SELECT_NOTE_AND_ANCESTORS_IDS_FOR_IDS)
    """)
    abstract fun updateDescendantsCountForNoteAndAncestors(ids: List<Long>)

    @Query("""
        UPDATE notes
        SET descendants_count = (
            SELECT count(*)
            FROM notes d
            WHERE (notes.book_id = d.book_id AND $WHERE_EXISTING_NOTES AND notes.lft < d.lft AND d.rgt < notes.rgt AND d.id NOT IN (:ignoreIds))
        )
        WHERE id IN ($SELECT_ANCESTORS_IDS_FOR_IDS)
    """)
    abstract fun updateDescendantsCountForAncestors(ids: Set<Long>, ignoreIds: Set<Long> = emptySet())

    @Query("UPDATE notes SET content = :content WHERE id = :id")
    abstract fun updateContent(id: Long, content: String?)

    @Query("""
        UPDATE notes
        SET title = :title, content = :content, state = :state, scheduled_range_id = :scheduled, deadline_range_id = :deadline, closed_range_id = :closed
        WHERE id = :id
    """)
    abstract fun update(id: Long, title: String, content: String?, state: String?, scheduled: Long?, deadline: Long?, closed: Long?): Int

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

    @Query("""
        UPDATE notes
        SET is_folded = 0, folded_under_id = 0
        WHERE id IN ($SELECT_SUBTREE_IDS_FOR_IDS)
    """)
    abstract fun unfoldSubtree(ids: List<Long>)

    @Query("""
        UPDATE notes
        SET is_folded = 1
        WHERE id IN ($SELECT_SUBTREE_IDS_FOR_IDS)
    """)
    abstract fun foldSubtree(ids: List<Long>)

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

    @Query("UPDATE notes SET lft = lft + :inc WHERE (book_id = :bookId AND $WHERE_EXISTING_NOTES) AND lft >= :value")
    abstract fun incrementLftForLftGe(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET lft = lft + :inc WHERE (book_id = :bookId AND $WHERE_EXISTING_NOTES) AND lft > :value")
    abstract fun incrementLftForLftGt(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET rgt = rgt + :inc WHERE book_id = :bookId AND is_cut = 0 AND rgt > :value")
    abstract fun incrementRgtForRgtGtOrRoot(bookId: Long, value: Long, inc: Int)

    @Query("UPDATE notes SET rgt = rgt + :inc WHERE book_id = :bookId AND is_cut = 0 AND rgt >= :value")
    abstract fun incrementRgtForRgtGeOrRoot(bookId: Long, value: Long, inc: Int)

    @Query("""
        UPDATE notes
        SET folded_under_id = 0
        WHERE id IN (:ids)
        AND folded_under_id NOT IN (:ids)
    """)
    abstract fun unfoldNotesFoldedUnderOthers(ids: Set<Long>)

    @Query("UPDATE notes SET folded_under_id = :foldedUnder WHERE id IN (:ids) AND folded_under_id = 0")
    abstract fun foldUnfolded(ids: Set<Long>, foldedUnder: Long)

    @Query("UPDATE notes SET folded_under_id = :parentId WHERE id = :noteId")
    abstract fun setFoldedUnder(noteId: Long, parentId: Long)

    @Query("UPDATE notes SET parent_id = :parentId WHERE id = :noteId")
    abstract fun updateParentForNote(noteId: Long, parentId: Long)

    @Query("""
        UPDATE notes
        SET book_id = :bookId, level = :level, lft = :lft, rgt = :rgt, parent_id = :parentId
        WHERE id = :noteId
    """)
    abstract fun updateNote(noteId: Long, bookId: Long, level: Int, lft: Long, rgt: Long, parentId: Long)

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
        SELECT notes.id as noteId, state, title, content, st.string AS scheduled, dt.string AS deadline

        FROM notes

        LEFT JOIN org_ranges sr ON (sr.id = notes.scheduled_range_id)
        LEFT JOIN org_timestamps st ON (st.id = sr.start_timestamp_id)

        LEFT JOIN org_ranges dr ON (dr.id = notes.deadline_range_id)
        LEFT JOIN org_timestamps dt ON (dt.id = dr.start_timestamp_id)

        WHERE notes.id IN (:ids) AND COALESCE(state, "") != COALESCE(:state, "")
    """)
    abstract fun getNoteForStateChange(ids: Set<Long>, state: String?): List<NoteForStateUpdate>

    @Query("""SELECT DISTINCT book_id FROM notes WHERE id IN (:ids) AND COALESCE(state, "") != COALESCE(:state, "")""")
    abstract fun getBookIdsForNotesNotMatchingState(ids: Set<Long>, state: String?): List<Long>

    @Query("SELECT MAX(rgt) FROM notes WHERE book_id = :bookId AND is_cut = 0")
    abstract fun getMaxRgtForBook(bookId: Long): Long?

    @Query("UPDATE notes SET created_at= :time WHERE id = :noteId")
    abstract fun updateCreatedAtTime(noteId: Long, time: Long)

    companion object {
        /* Every book has a root note with level 0. */
        const val WHERE_EXISTING_NOTES = "(is_cut = 0 AND level > 0)"

        @Language("RoomSql")
        const val SELECT_ANCESTORS_IDS_FOR_IDS = """
            SELECT DISTINCT a.id
            FROM notes n, notes a
            WHERE n.id IN (:ids)
            AND n.book_id = a.book_id
            AND a.is_cut = 0
            AND a.level > 0
            AND a.lft < n.lft
            AND n.rgt < a.rgt
            """

        @Language("RoomSql")
        const val SELECT_NOTE_AND_ANCESTORS_IDS_FOR_IDS = """
            SELECT DISTINCT a.id
            FROM notes n, notes a
            WHERE n.id IN (:ids)
            AND n.book_id = a.book_id
            AND a.is_cut = 0
            AND a.level > 0
            AND a.lft <= n.lft
            AND n.rgt <= a.rgt
            """

        @Language("RoomSql")
        private const val SELECT_SUBTREE_FOR_IDS = """
            SELECT d.*
            FROM notes n, notes d
            WHERE n.id IN (:ids)
            AND n.level > 0
            AND d.book_id = n.book_id
            AND d.is_cut = 0
            AND n.is_cut = 0
            AND n.lft <= d.lft
            AND d.rgt <= n.rgt
            GROUP BY d.id
            ORDER BY d.lft
            """

        @Language("RoomSql")
        private const val SELECT_SUBTREE_IDS_FOR_IDS = """
            SELECT DISTINCT d.id
            FROM notes n, notes d
            WHERE n.id IN (:ids)
            AND d.book_id = n.book_id
            AND d.is_cut = 0
            AND n.lft <= d.lft
            AND d.rgt <= n.rgt
            """

        fun rootNote(bookId: Long): Note {
            return Note(id = 0, position = NotePosition(bookId, lft = 1, rgt = 2, level = 0))
        }
    }

    data class NoteIdBookId(val noteId: Long, val bookId: Long)

    data class NoteForStateUpdate(
            val noteId: Long,
            val state: String?,
            val title: String,
            val content: String?,
            val scheduled: String?,
            val deadline: String?)
}
