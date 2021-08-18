package com.orgzly.android.external.actionhandlers

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

    private fun getNote(intent: Intent) =
            intent.getNote()
                ?.let { Response(true, Note.from(it)) }
                ?: Response(false, "Couldn't find note at specified path!")
}