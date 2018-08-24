package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "book_links",

        foreignKeys = [
            ForeignKey(
                    entity = Book::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("book_id"),
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(
                    entity = Repo::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("repo_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("repo_id")
        ]
)
data class BookLink(
        @PrimaryKey
        @ColumnInfo(name = "book_id")
        val bookId: Long,

        @ColumnInfo(name = "repo_id")
        val repoId: Long
)
