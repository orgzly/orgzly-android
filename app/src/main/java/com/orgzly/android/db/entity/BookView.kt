package com.orgzly.android.db.entity

import androidx.room.Embedded
import com.orgzly.android.repos.VersionedRook

data class BookView(
        @Embedded
        val book: Book,

        val noteCount: Int,

        @Embedded(prefix = "link_repo_")
        val linkRepo: Repo? = null,

        @Embedded(prefix = "synced_to_")
        val syncedTo: VersionedRook? = null
) {
    fun hasLink(): Boolean {
        return linkRepo != null
    }

    fun hasSync(): Boolean {
        return syncedTo != null
    }

    fun isOutOfSync(): Boolean {
        return syncedTo != null && book.isModified
    }

    fun isModified(): Boolean {
        return book.isModified
    }
}