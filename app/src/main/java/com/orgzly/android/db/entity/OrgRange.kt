package com.orgzly.android.db.entity

import androidx.room.*

@Entity(
        tableName = "org_ranges",

        foreignKeys = [
            ForeignKey(
                    entity = OrgTimestamp::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("start_timestamp_id"),
                    onDelete = ForeignKey.CASCADE),
            ForeignKey(
                    entity = OrgTimestamp::class,
                    parentColumns = arrayOf("id"),
                    childColumns = arrayOf("end_timestamp_id"),
                    onDelete = ForeignKey.CASCADE)
        ],

        indices = [
            Index("string", unique = true),
            Index("start_timestamp_id"),
            Index("end_timestamp_id")
        ]
)
data class OrgRange(
        @PrimaryKey(autoGenerate = true)
        val id: Long,

        val string: String,

        @ColumnInfo(name = "start_timestamp_id")
        val startTimestampId: Long,

        @ColumnInfo(name = "end_timestamp_id")
        val endTimestampId: Long? = null,

        val difference: Long? = null
)
