package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "repos",

        indices = [
            Index("url", unique = true)
        ]
)
data class Repo(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = "url")
        val url: String
) {
        override fun toString(): String {
            return url
        }
}