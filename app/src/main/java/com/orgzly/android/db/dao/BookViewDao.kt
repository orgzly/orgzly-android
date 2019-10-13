package com.orgzly.android.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.orgzly.android.db.entity.BookView
import org.intellij.lang.annotations.Language

@Dao
abstract class BookViewDao {
    @Query("$QUERY WHERE books.id = :id GROUP BY books.id")
    abstract fun get(id: Long): BookView?

    @Query("$QUERY WHERE books.name = :name GROUP BY books.id")
    abstract fun get(name: String): BookView?

    @Query("$QUERY GROUP BY books.id ORDER BY $ORDER_BY_NAME")
    abstract fun getAllFOrderByNameLiveData(): LiveData<List<BookView>>

    @Query("$QUERY GROUP BY books.id ORDER BY $ORDER_BY_TIME")
    abstract fun getAllOrderByTimeLiveData(): LiveData<List<BookView>>

    @Query("$QUERY GROUP BY books.id ORDER BY $ORDER_BY_NAME")
    abstract fun getAllFOrderByName(): List<BookView>

    @Query("$QUERY GROUP BY books.id ORDER BY $ORDER_BY_TIME")
    abstract fun getAllOrderByTime(): List<BookView>

    companion object {
        @Language("RoomSql")
        private const val QUERY = """
            SELECT

            books.*,

            count(notes.id) as noteCount,

            link_repos.id as link_repo_id,
            link_repos.type as link_repo_type,
            link_repos.url as link_repo_url,

            synced_repos.id as synced_to_repoId,
            synced_repos.type as synced_to_repoType,
            synced_repos.url as synced_to_repoUri,
            synced_rook_urls.url as synced_to_uri,
            synced_versioned_rooks.rook_revision as synced_to_revision,
            synced_versioned_rooks.rook_mtime as synced_to_mtime

            FROM books

            LEFT JOIN notes ON (books.id = notes.book_id AND notes.is_cut = 0 AND notes.level > 0)

            LEFT JOIN book_links ON (books.id = book_links.book_id)
            LEFT JOIN repos AS link_repos ON (book_links.repo_id = link_repos.id)

            LEFT JOIN book_syncs ON (books.id = book_syncs.book_id)
            LEFT JOIN versioned_rooks AS synced_versioned_rooks ON (book_syncs.versioned_rook_id = synced_versioned_rooks.id)
            LEFT JOIN rooks AS synced_rooks ON (synced_versioned_rooks.rook_id = synced_rooks.id)
            LEFT JOIN repos AS synced_repos ON (synced_rooks.repo_id = synced_repos.id)
            LEFT JOIN rook_urls AS synced_rook_urls ON (synced_rooks.rook_url_id = synced_rook_urls.id)
        """

        private const val ORDER_BY_TIME = "is_dummy, MAX(COALESCE(mtime, 0), COALESCE(synced_to_mtime, 0)) DESC, name"

        private const val ORDER_BY_NAME = "is_dummy, LOWER(COALESCE(books.title, name))"
    }
}