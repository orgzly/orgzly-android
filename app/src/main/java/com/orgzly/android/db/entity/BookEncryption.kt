package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "book_encryptions",

        foreignKeys = [
            ForeignKey(
                    entity = Book::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("book_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            // todo needed
        ]
)
data class BookEncryption(
        @PrimaryKey
        @ColumnInfo(name = "book_id")
        val bookId: Long,

        @ColumnInfo(name = "passphrase")
        val passphrase: String
)
