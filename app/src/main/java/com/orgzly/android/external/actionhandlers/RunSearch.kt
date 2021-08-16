package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.external.types.*

class RunSearch : ExternalAccessActionHandler() {
    override val actions = mapOf(
            "SEARCH" to ::runSearch
    )

    @Suppress("UNUSED_PARAMETER")
    private fun runSearch(intent: Intent, context: Context): Response {
        val searchTerm = intent.getStringExtra("QUERY")
        if (searchTerm.isNullOrBlank()) return Response(false, "Invalid search term!")
        val query = InternalQueryParser().parse(searchTerm)
        val notes = dataRepository.selectNotesFromQuery(query)
        return Response(true, notes.map(Note::from).toTypedArray())
    }
}