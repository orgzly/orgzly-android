package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.OrgRange

@Dao
interface OrgRangeDao : BaseDao<OrgRange> {
    @Query("SELECT * FROM org_ranges WHERE string = :str")
    fun getByString(str: String): OrgRange?
}
