package com.orgzly.android.db.dao

import androidx.room.*
import com.orgzly.android.db.entity.BookLink

@Dao
abstract class BookLinkDao : BaseDao<BookLink> {
    @Query("SELECT * FROM book_links WHERE book_id = :bookId")
    abstract fun getByBookId(bookId: Long): BookLink?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun replace(bookLink: BookLink): Long

    @Query("DELETE FROM book_links WHERE book_id = :bookId")
    abstract fun deleteByBookId(bookId: Long)

    @Query("DELETE FROM book_links WHERE repo_id = :repoId")
    abstract fun deleteByRepoId(repoId: Long)

    @Transaction
    open fun upsert(bookId: Long, repoId: Long) {
        val link = getByBookId(bookId)

        if (link == null) {
            insert(BookLink(bookId, repoId))
        } else {
            update(link.copy(repoId = repoId))
        }
    }
}
