package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.external.types.Response

class EditSavedSearches : ExternalAccessActionHandler() {
    override val actions = listOf(
            action(::addSavedSearch, "ADD_SAVED_SEARCH"),
            action(::editSavedSearch, "EDIT_SAVED_SEARCH"),
            action(::moveSavedSearch, "MOVE_SAVED_SEARCH"),
            action(::deleteSavedSearch, "DELETE_SAVED_SEARCH"),
    )

    private fun addSavedSearch(intent: Intent) =
            intent.getNewSavedSearch()?.let {
                val id = dataRepository.createSavedSearch(it)
                Response(true, "$id")
            } ?: Response(false, "Invalid saved search details")

    private fun editSavedSearch(intent: Intent) = intent.getSavedSearch()?.let { savedSearch ->
        intent.getNewSavedSearch(allowBlank = true)?.let { newSavedSearch ->
            dataRepository.updateSavedSearch(SavedSearch(
                    savedSearch.id,
                    (if (newSavedSearch.name.isBlank()) savedSearch.name
                    else newSavedSearch.name),
                    (if (newSavedSearch.query.isBlank()) savedSearch.query
                    else newSavedSearch.query),
                    savedSearch.position
            ))
            return Response()
        } ?: Response(false, "Invalid saved search details")
    } ?: Response(false, "Couldn't find saved search")

    private fun moveSavedSearch(intent: Intent) = intent.getSavedSearch()?.let { savedSearch ->
        when (intent.getStringExtra("DIRECTION")) {
            "UP" -> dataRepository.moveSavedSearchUp(savedSearch.id)
            "DOWN" -> dataRepository.moveSavedSearchDown(savedSearch.id)
            else -> return Response(false, "Invalid direction")
        }
        return Response()
    } ?: Response(false, "Couldn't find saved search")

    private fun deleteSavedSearch(intent: Intent) = intent.getSavedSearch()?.let { savedSearch ->
        dataRepository.deleteSavedSearches(setOf(savedSearch.id))
        return Response()
    }  ?: Response(false, "Couldn't find saved search")
}