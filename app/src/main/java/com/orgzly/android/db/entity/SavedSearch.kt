package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "searches"
)
data class SavedSearch(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        val name: String,

        val query: String,

        val position: Int
) {
    fun areContentsTheSame(that: SavedSearch): Boolean {
        return name == that.name && query == that.query && position == that.position
    }
}