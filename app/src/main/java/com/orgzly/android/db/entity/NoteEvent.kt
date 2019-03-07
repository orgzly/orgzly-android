package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "note_events",

        primaryKeys = [ "note_id", "org_range_id" ],

        foreignKeys = [
            ForeignKey(
                    entity = Note::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("note_id"),
                    onDelete = ForeignKey.CASCADE),

            ForeignKey(
                    entity = OrgRange::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("org_range_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("note_id"),
            Index("org_range_id")
        ]
)
data class NoteEvent(
        @ColumnInfo(name = "note_id")
        val noteId: Long,

        @ColumnInfo(name = "org_range_id")
        val orgRangeId: Long
)
