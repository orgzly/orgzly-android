package com.orgzly.android.db.entity

import androidx.room.*
import android.text.TextUtils
import com.google.gson.Gson



@Entity(
        tableName = "notes",

        foreignKeys = [
            ForeignKey(
                    entity = Book::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("book_id"),
                    onDelete = ForeignKey.CASCADE),

            ForeignKey(
                    entity = OrgRange::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("scheduled_range_id"),
                    onDelete = ForeignKey.CASCADE),

            ForeignKey(
                    entity = OrgRange::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("deadline_range_id"),
                    onDelete = ForeignKey.CASCADE),

            ForeignKey(
                    entity = OrgRange::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("closed_range_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("title"),
            Index("tags"),
            Index("content"),
            Index("book_id"),
            Index("is_cut"),
            Index("lft"),
            Index("rgt"),
            Index("is_folded"),
            Index("folded_under_id"),
            Index("parent_id"),
            Index("descendants_count"),
            Index("scheduled_range_id"),
            Index("deadline_range_id"),
            Index("closed_range_id")
        ]
)
data class Note(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        @ColumnInfo(name = "is_cut")
        val isCut: Long = 0,

        @ColumnInfo(name = "created_at")
        val createdAt: Long? = null,

        val title: String = "",

        val tags: String? = null,

        val state: String? = null,

        val priority: String? = null,

        val content: String? = null,

        @ColumnInfo(name = "content_line_count")
        val contentLineCount: Int = 0,

        @ColumnInfo(name = "scheduled_range_id")
        val scheduledRangeId: Long? = null,

        @ColumnInfo(name = "deadline_range_id")
        val deadlineRangeId: Long? = null,

        @ColumnInfo(name = "closed_range_id")
        val closedRangeId: Long? = null,

        @ColumnInfo(name = "clock_range_id")
        val clockRangeId: Long? = null,

        @Embedded(prefix = "")
        val position: NotePosition
) {

    fun hasContent(): Boolean {
        return content != null && !content.isEmpty()
    }

    fun hasTags(): Boolean {
        return tags != null && !tags.isEmpty()
    }

    fun getTagsList(): List<String> {
        return dbDeSerializeTags(tags)
    }

    fun toJson(): String {
        val note = Note(0, position = NotePosition(0))
        val gson = Gson()
        return gson.toJson(note)
    }

    companion object {
        @JvmStatic
        fun dbSerializeTags(tags: List<String>): String {
            return TextUtils.join(" ", tags)
        }

        @JvmStatic
        fun dbDeSerializeTags(str: String?): List<String> {
            return str?.split(" +".toRegex()) ?: emptyList()
        }
    }
}