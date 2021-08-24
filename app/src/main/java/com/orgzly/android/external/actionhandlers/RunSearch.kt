package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.orgzly.android.external.types.ExternalHandlerFailure
import com.orgzly.android.external.types.Note
import com.orgzly.android.query.user.InternalQueryParser

class RunSearch : ExternalAccessActionHandler() {
    override val actions = listOf(
        action(::runSearch, "SEARCH")
    )

    private fun runSearch(intent: Intent): List<Note> {
        val searchTerm = intent.getStringExtra("QUERY")
        if (searchTerm.isNullOrBlank()) throw ExternalHandlerFailure("invalid search term")
        val query = InternalQueryParser().parse(searchTerm)
        val notes = dataRepository.selectNotesFromQuery(query)
        return notes.map(Note::from)
    }
}