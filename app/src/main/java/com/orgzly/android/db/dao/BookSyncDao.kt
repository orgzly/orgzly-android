package com.orgzly.android.db.dao

import androidx.room.*
import com.orgzly.android.db.entity.BookSync

@Dao
interface BookSyncDao : BaseDao<BookSync> {
    @Query("SELECT * FROM book_syncs WHERE book_id = :bookId")
    fun get(bookId: Long): BookSync?

    @Transaction
    fun upsert(bookId: Long, versionedRookId: Long) {
        val sync = get(bookId)

        if (sync == null) {
            insert(BookSync(bookId, versionedRookId))
        } else {
            update(sync.copy(versionedRookId = versionedRookId))
        }
    }
}
