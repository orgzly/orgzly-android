package com.orgzly.android.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.BookAction


@Dao
abstract class BookDao : BaseDao<Book> {
    @Query("SELECT * FROM books WHERE id = :id")
    abstract fun get(id: Long): Book?

    @Query("SELECT * FROM books WHERE name = :name")
    abstract fun get(name: String): Book?

    @Query("SELECT * FROM books WHERE id = :id")
    abstract fun getLiveData(id: Long): LiveData<Book> // null not allowed, use List

    @Insert
    abstract fun insertBooks(vararg books: Book): LongArray

    @Update
    abstract fun updateBooks(vararg book: Book): Int

    @Query("UPDATE books SET preface = :preface, title = :title WHERE id = :id")
    abstract fun updatePreface(id: Long, preface: String?, title: String?)

    @Query("UPDATE books SET last_action_type = :type, last_action_message = :message, last_action_timestamp = :timestamp WHERE id = :id")
    abstract fun updateLastAction(id: Long, type: BookAction.Type, message: String, timestamp: Long)

    @Query("UPDATE books SET last_action_type = :type, last_action_message = :message, last_action_timestamp = :timestamp, sync_status = :status WHERE id = :id")
    abstract fun updateLastActionAndSyncStatus(id: Long, type: BookAction.Type, message: String, timestamp: Long, status: String?): Int

    @Query("UPDATE books SET name = :name WHERE id = :id")
    abstract fun updateName(id: Long, name: String): Int

    @Query("UPDATE books SET is_dummy = :dummy WHERE id = :id")
    abstract fun updateDummy(id: Long, dummy: Boolean)

    @Query("UPDATE books SET mtime = :mtime, is_modified = 1 WHERE id IN (:ids)")
    abstract fun setIsModified(ids: Set<Long>, mtime: Long): Int

    @Query("UPDATE books SET is_modified = 0 WHERE id IN (:ids)")
    abstract fun setIsNotModified(ids: Set<Long>): Int

    fun getOrInsert(name: String): Long =
            get(name).let {
                it?.id ?: insert(Book(0, name, isDummy = true))
            }
}
