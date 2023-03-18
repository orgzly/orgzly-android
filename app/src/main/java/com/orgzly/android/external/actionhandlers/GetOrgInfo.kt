package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.orgzly.android.external.types.*

class GetOrgInfo : ExternalAccessActionHandler() {
    override val actions = listOf(
        action(::getBooks, "GET_BOOKS"),
        action(::getSavedSearches, "GET_SAVED_SEARCHES"),
        action(::getNote, "GET_NOTE")
    )

    private fun getBooks() =
            dataRepository.getBooks().map(Book::from).toTypedArray()

    private fun getSavedSearches() =
            dataRepository.getSavedSearches().map(SavedSearch::from).toTypedArray()

    private fun getNote(intent: Intent) =
            Note.from(intent.getNoteAndProps())
}
