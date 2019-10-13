package com.orgzly.android.db.entity

import androidx.room.Entity

/**
 * Last action performed on a notebook.
 *
 * Action can be a result of renaming, syncing (loading, saving, ...), importing, etc.
 */
data class BookAction(
        val type: Type,
        val message: String,
        val timestamp: Long
) {
    enum class Type {
        INFO,
        ERROR,
        PROGRESS
    }

    companion object {
        @JvmStatic
        fun forNow(type: Type, message: String): BookAction {
            return BookAction(type, message, System.currentTimeMillis())
        }
    }
}