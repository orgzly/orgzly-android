package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.RookUrl

@Dao
abstract class RookUrlDao : BaseDao<RookUrl> {
    @Query("SELECT * FROM rook_urls WHERE url = :url")
    abstract fun get(url: String): RookUrl?

    fun getOrInsert(rookUrl: String): Long =
            get(rookUrl).let {
                it?.id ?: insert(RookUrl(0, rookUrl))
            }
}
