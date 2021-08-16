package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.orgzly.android.external.types.*

class GetOrgInfo : ExternalAccessActionHandler() {
    override val actions = mapOf(
            "GET_BOOKS" to ::getBooks,
            "GET_SAVED_SEARCHES" to ::getSavedSearches,
            "GET_NOTE" to ::getNote
    )

    @Suppress("UNUSED_PARAMETER")
    private fun getBooks(intent: Intent, context: Context) = Response(
            true,
            dataRepository.getBooks()
                    .map(Book::from).toTypedArray()
    )

    @Suppress("UNUSED_PARAMETER")
    private fun getSavedSearches(intent: Intent, context: Context) = Response(
            true,
            dataRepository.getSavedSearches()
                    .map(SavedSearch::from).toTypedArray()
    )

    @Suppress("UNUSED_PARAMETER")
    private fun getNote(intent: Intent, context: Context): Response {
        val bookName = intent.getStringExtra("BOOK_NAME")
        val path = intent.getStringExtra("PATH")
        return if (bookName == null || path == null) {
            Response(false, "Invalid arguments!")
        } else {
            dataRepository.getNoteAtPath(bookName, path)
                    ?.let { Response(true, Note.from(it)) }
                    ?: Response(false, "Couldn't find note at specified path!")
        }
    }
}