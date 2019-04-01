package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "note_properties",

        primaryKeys = [ "note_id", "position" ],

        foreignKeys = [
            ForeignKey(
                    entity = Note::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("note_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("note_id"),
            Index("position"),
            Index("name"),
            Index("value")
        ]
)
data class NoteProperty(
        @ColumnInfo(name = "note_id")
        val noteId: Long,

        val position: Int,

        val name: String,

        val value: String
)
