package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.external.types.ExternalHandlerFailure

class EditSavedSearches : ExternalAccessActionHandler() {
    override val actions = listOf(
            action(::addSavedSearch, "ADD_SAVED_SEARCH"),
            action(::editSavedSearch, "EDIT_SAVED_SEARCH"),
            action(::moveSavedSearch, "MOVE_SAVED_SEARCH"),
            action(::deleteSavedSearch, "DELETE_SAVED_SEARCH"),
    )

    private fun addSavedSearch(intent: Intent): String {
        val savedSearch = intent.getNewSavedSearch()
        val id = dataRepository.createSavedSearch(savedSearch)
        return "$id"
    }

    private fun editSavedSearch(intent: Intent) {
        val savedSearch = intent.getSavedSearch()
        val newSavedSearch = intent.getNewSavedSearch(allowBlank = true)
        dataRepository.updateSavedSearch(SavedSearch(
                savedSearch.id,
                newSavedSearch.name.ifBlank { savedSearch.name },
                newSavedSearch.query.ifBlank { savedSearch.query },
                savedSearch.position
        ))
    }

    private fun moveSavedSearch(intent: Intent) {
        val savedSearch = intent.getSavedSearch()
        when (intent.getStringExtra("DIRECTION")) {
            "UP" -> dataRepository.moveSavedSearchUp(savedSearch.id)
            "DOWN" -> dataRepository.moveSavedSearchDown(savedSearch.id)
            else -> throw ExternalHandlerFailure("invalid direction")
        }
    }

    private fun deleteSavedSearch(intent: Intent) {
        val savedSearch = intent.getSavedSearch()
        dataRepository.deleteSavedSearches(setOf(savedSearch.id))
    }
}
