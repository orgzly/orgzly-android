package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.orgzly.android.external.types.*

class GetOrgInfo : ExternalAccessActionHandler() {
    override val actions = listOf(
        action(::getBooks, "GET_BOOKS"),
        action(::getSavedSearches, "GET_SAVED_SEARCHES"),
        action(::getNote, "GET_NOTE")
    )

    private fun getBooks() = Response(
            true,
            dataRepository.getBooks()
                    .map(Book::from).toTypedArray()
    )

    private fun getSavedSearches() = Response(
            true,
            dataRepository.getSavedSearches()
                    .map(SavedSearch::from).toTypedArray()
    )

    private fun getNote(intent: Intent): Response {
        val book = getBook(intent) ?: return Response(false, "Couldn't find specified book")
        val path = intent.getStringExtra("PATH")
                ?: return Response(false, "Invalid arguments!")
        return dataRepository.getNoteAtPath(book.name, path)
                ?.let { Response(true, Note.from(it)) }
                ?: Response(false, "Couldn't find note at specified path!")
    }
}