package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.Rook

@Dao
abstract class RookDao : BaseDao<Rook> {
    @Query("SELECT * FROM rooks WHERE repo_id = :repoId AND rook_url_id = :rookUrlId")
    abstract fun get(repoId: Long, rookUrlId: Long): Rook?

    fun getOrInsert(repoId: Long, rookUrlId: Long): Long =
            get(repoId, rookUrlId).let {
                it?.id ?: insert(Rook(0, repoId, rookUrlId))
            }
}
