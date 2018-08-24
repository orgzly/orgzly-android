package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "db_repo_books",

        indices = [
            Index("repo_url", "url", unique = true)
        ]
)
data class DbRepoBook(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = "repo_url")
        val repoUrl: String,

        val url: String,

        val revision: String,

        val mtime: Long,

        val content: String,

        @ColumnInfo(name = "created_at")
        val createdAt: Long
)
