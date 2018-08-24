package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "note_ancestors",

        primaryKeys = [ "book_id", "note_id", "ancestor_note_id" ],

        foreignKeys = [
            ForeignKey(
                    entity = Book::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("book_id"),
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(
                    entity = Note::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("note_id"),
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(
                    entity = Note::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("ancestor_note_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("book_id"),
            Index("note_id"),
            Index("ancestor_note_id")
        ]
)
data class NoteAncestor(
        @ColumnInfo(name = "note_id")
        val noteId: Long,

        @ColumnInfo(name = "book_id")
        val bookId: Long,

        @ColumnInfo(name = "ancestor_note_id")
        val ancestorNoteId: Long
)
