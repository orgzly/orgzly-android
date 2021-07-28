package com.orgzly.android.db.dao

import androidx.room.*
import com.orgzly.android.db.entity.BookEncryption
import com.orgzly.android.db.entity.BookLink

@Dao
abstract class BookEncryptionDao : BaseDao<BookEncryption> {
    @Query("SELECT * FROM book_encryptions WHERE book_id = :bookId")
    abstract fun getByBookId(bookId: Long): BookEncryption?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun replace(bookEncryption: BookEncryption): Long

    @Query("DELETE FROM book_encryptions WHERE book_id = :bookId")
    abstract fun deleteByBookId(bookId: Long)

    @Transaction
    open fun upsert(bookId: Long, passphrase: String) {
        val enc = getByBookId(bookId)

        if (enc == null) {
            insert(BookEncryption(bookId, passphrase))
        } else {
            update(enc.copy(passphrase = passphrase))
        }
    }
}