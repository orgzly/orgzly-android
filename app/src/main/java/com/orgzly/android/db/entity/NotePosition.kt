package com.orgzly.android.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Note's position in the book.
 */
data class NotePosition(
        @ColumnInfo(name = "book_id")
        val bookId: Long,

        /** Nested set model's left and right values  */
        val lft: Long = 0,
        val rgt: Long = 0,

        /** Level (depth) of a note  */
        val level: Int = 0,

        /** ID of the parent note */
        @ColumnInfo(name = "parent_id")
        val parentId: Long = 0,

        /** ID of the folded note which hides this one */
        @ColumnInfo(name = "folded_under_id")
        val foldedUnderId: Long = 0,

        /** Is the note folded (collapsed) or unfolded (expanded)  */
        @ColumnInfo(name = "is_folded")
        val isFolded: Boolean = false,

        /** Number of descendants */
        @ColumnInfo(name = "descendants_count")
        val descendantsCount: Int = 0
)