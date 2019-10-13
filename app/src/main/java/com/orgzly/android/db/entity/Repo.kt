package com.orgzly.android.db.entity

import androidx.room.*
import com.orgzly.android.repos.RepoType

@Entity(
        tableName = "repos",

        indices = [
            Index("url", unique = true)
        ]
)
data class Repo(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        val type: RepoType,

        val url: String
) {
        override fun toString(): String {
            return url
        }
}