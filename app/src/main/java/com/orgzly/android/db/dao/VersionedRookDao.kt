package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.orgzly.android.db.entity.VersionedRook

@Dao
abstract class VersionedRookDao : BaseDao<VersionedRook> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun replace(rook: VersionedRook): Long
}
