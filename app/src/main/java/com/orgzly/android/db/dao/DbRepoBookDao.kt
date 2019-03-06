package com.orgzly.android.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.DbRepoBook

@Dao
interface DbRepoBookDao : BaseDao<DbRepoBook> {
    @Query("SELECT * FROM db_repo_books WHERE url = :url")
    fun getByUrl(url: String): DbRepoBook?

    @Query("SELECT * FROM db_repo_books WHERE repo_url = :repoUrl")
    fun getAllByRepo(repoUrl: String): List<DbRepoBook>

    @Query("DELETE FROM db_repo_books WHERE url = :url")
    fun deleteByUrl(url: String): Int
}
