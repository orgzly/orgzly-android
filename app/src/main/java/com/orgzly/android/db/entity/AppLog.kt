package com.orgzly.android.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
        tableName = "app_logs",

        indices = [
                Index("timestamp"),
                Index("name")
        ]
)
data class AppLog(
        @PrimaryKey(autoGenerate = true)

        val id: Long,

        val timestamp: Long,

        val name: String,

        val message: String
)