package com.orgzly.android.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.orgzly.android.db.entity.Repo

@Dao
abstract class RepoDao : BaseDao<Repo> {
    @Query("SELECT * FROM repos WHERE url = :url")
    abstract fun get(url: String): Repo?

    @Query("SELECT * FROM repos WHERE id = :id")
    abstract fun get(id: Long): Repo?

    @Query("SELECT * FROM repos WHERE id = :id")
    abstract fun getLiveData(id: Long): LiveData<Repo>

    @Query("SELECT * FROM repos ORDER BY url")
    abstract fun getAll(): List<Repo>

    @Query("SELECT * FROM repos ORDER BY url")
    abstract fun getAllLiveData(): LiveData<List<Repo>>

    @Query("DELETE FROM repos WHERE id = :id")
    abstract fun delete(id: Long)

    @Transaction
    open fun deleteAndInsert(repo: Repo): Long {
        delete(repo.id)

        return insert(Repo(0, repo.type, repo.url))
    }
}
