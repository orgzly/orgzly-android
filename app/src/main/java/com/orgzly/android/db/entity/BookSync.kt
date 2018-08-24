package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "book_syncs",

        foreignKeys = [
            ForeignKey(
                    entity = Book::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("book_id"),
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(
                    entity = VersionedRook::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("versioned_rook_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("versioned_rook_id")
        ]
)
data class BookSync(
        @PrimaryKey
        @ColumnInfo(name = "book_id")
        val bookId: Long,

        @ColumnInfo(name = "versioned_rook_id")
        val versionedRookId: Long
)
