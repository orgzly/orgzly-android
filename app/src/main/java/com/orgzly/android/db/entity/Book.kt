package com.orgzly.android.db.entity


import androidx.room.*


@Entity(
        tableName = "books",

        indices = [
            Index("name", unique = true)
        ]
)
data class Book constructor(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        val name: String,

        val title: String? = null,

        val mtime: Long? = null,

        @ColumnInfo(name = "is_dummy")
        val isDummy: Boolean = false,

        @ColumnInfo(name = "is_deleted")
        val isDeleted: Boolean? = false,

        val preface: String? = null,

        @ColumnInfo(name = "is_indented")
        val isIndented: Boolean? = false,

        @ColumnInfo(name = "used_encoding")
        val usedEncoding: String? = null,

        @ColumnInfo(name = "detected_encoding")
        val detectedEncoding: String? = null,

        @ColumnInfo(name = "selected_encoding")
        val selectedEncoding: String? = null,

        @ColumnInfo(name = "sync_status")
        val syncStatus: String? = null, // TODO: BookSyncStatus

        @Embedded(prefix = "last_action_")
        val lastAction: BookAction? = null,

        @ColumnInfo(name = "is_modified")
        val isModified: Boolean = false
) {

    override fun toString(): String {
        return "$name#$id"
    }

    companion object {
        @JvmStatic
        fun forName(name: String): Book {
            return Book(0, name)
        }
    }
}
