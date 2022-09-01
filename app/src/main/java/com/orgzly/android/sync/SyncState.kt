package com.orgzly.android.sync

import android.content.Context
import androidx.work.Data
import androidx.work.workDataOf
import com.orgzly.R

data class SyncState(val type: Type, val message: String? = null, val current: Int, val total: Int) {
    enum class Type {
        CANCELING,

        STARTING,
        COLLECTING_BOOKS,
        BOOKS_COLLECTED,

        BOOK_STARTED,
        BOOK_ENDED,

        AUTO_SYNC_NOT_STARTED,
        FINISHED,
        CANCELED,

        FAILED_NO_REPOS,
        FAILED_NO_CONNECTION,
        FAILED_NO_STORAGE_PERMISSION,
        FAILED_NO_BOOKS_FOUND,
        FAILED_EXCEPTION
    }

    fun isFailure(): Boolean {
        return when (type) {
            Type.FAILED_NO_REPOS,
            Type.FAILED_NO_CONNECTION,
            Type.FAILED_NO_STORAGE_PERMISSION,
            Type.FAILED_NO_BOOKS_FOUND,
            Type.FAILED_EXCEPTION ->
                true
            else ->
                false
        }
    }

    fun isSuccess(): Boolean {
        return when (type) {
            Type.AUTO_SYNC_NOT_STARTED,
            Type.FINISHED,
            Type.CANCELED ->
                true
            else ->
                false
        }
    }

    fun isRunning(): Boolean {
        return !isSuccess() && !isFailure()
    }

    fun toData(): Data {
        return workDataOf(
            DATA_TYPE to type.toString(),
            DATA_MESSAGE to message,
            DATA_CURRENT to current,
            DATA_TOTAL to total
        )
    }

    fun getDescription(context: Context): String? {
        with(context.resources) {
            return when (type) {
                Type.CANCELING -> getString(R.string.canceling)

                Type.STARTING -> getString(R.string.syncing_in_progress)
                Type.COLLECTING_BOOKS -> getString(R.string.collecting_notebooks_in_progress)
                Type.BOOKS_COLLECTED -> getString(R.string.syncing_in_progress)

                Type.BOOK_STARTED -> getString(R.string.syncing_book, message)
                Type.BOOK_ENDED -> getString(R.string.syncing_in_progress)

                Type.AUTO_SYNC_NOT_STARTED -> null
                Type.FINISHED -> null
                Type.CANCELED -> getString(R.string.last_sync_with_argument, getString(R.string.canceled))

                Type.FAILED_NO_REPOS -> getString(R.string.no_repos)
                Type.FAILED_NO_CONNECTION -> getString(R.string.no_connection)
                Type.FAILED_NO_STORAGE_PERMISSION -> getString(R.string.storage_permissions_missing)
                Type.FAILED_NO_BOOKS_FOUND -> getString(R.string.no_books)
                Type.FAILED_EXCEPTION -> message
            }
        }
    }

//    @SuppressLint("ApplySharedPref")
//    fun saveToPreferences(context: Context) {
//        context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
//            .edit()
//            .putString(DATA_TYPE, type.toString())
//            .putString(DATA_MESSAGE, message)
//            .putInt(DATA_CURRENT, current)
//            .putInt(DATA_TOTAL, total)
//            .commit()
//            // .apply()
//    }

    override fun toString(): String {
        return "$type($message $current/$total)"
    }

    companion object {
        private const val DATA_TYPE = "type"
        private const val DATA_MESSAGE = "message"
        private const val DATA_CURRENT = "current"
        private const val DATA_TOTAL = "total"

//        private const val SHARED_PREF_NAME = "sync-state"

//        fun fromPreferences(context: Context): SyncState? {
//            val pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
//
//            val type = pref.getString(DATA_TYPE, null) ?: return null
//            val message = pref.getString(DATA_MESSAGE, null)
//            val current = pref.getInt(DATA_CURRENT, 0)
//            val total = pref.getInt(DATA_TOTAL, 0)
//
//            return SyncState(Type.valueOf(type), message, current, total)
//        }

        @JvmStatic
        @JvmOverloads
        fun getInstance(type: Type, message: String? = null, current: Int = 0, total: Int = 0): SyncState {
            return SyncState(type, message, current, total)
        }

        @JvmStatic
        fun fromData(data: Data): SyncState? {
            val type = data.getString(DATA_TYPE) ?: return null
            val message = data.getString(DATA_MESSAGE)
            val current = data.getInt(DATA_CURRENT, 0)
            val total = data.getInt(DATA_TOTAL, 0)

            return SyncState(Type.valueOf(type), message, current, total)
        }
    }
}