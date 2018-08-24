package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "rook_urls",

        indices = [
            Index("url", unique = true)
        ]
)
data class RookUrl(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        val url: String
)
