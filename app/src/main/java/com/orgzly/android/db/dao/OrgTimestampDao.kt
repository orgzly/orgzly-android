package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.OrgTimestamp

@Dao
interface OrgTimestampDao : BaseDao<OrgTimestamp> {
    @Query("SELECT * FROM org_timestamps")
    fun getAll(): List<OrgTimestamp>

    @Query("SELECT * FROM org_timestamps WHERE string = :str")
    fun getByString(str: String): OrgTimestamp?
}
