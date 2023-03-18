package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.external.types.ExternalHandlerFailure
import com.orgzly.android.query.user.InternalQueryParser
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.org.OrgProperties

interface ExternalIntentParser {
    val dataRepository: DataRepository

    fun Intent.getNotePayload(title: String? = null): NotePayload {
        val rawJson = getStringExtra("NOTE_PAYLOAD")
        val json = try {
            JsonParser.parseString(rawJson)
                .let { if (it.isJsonObject) it.asJsonObject else null }!!
        } catch (e: Exception) {
            throw ExternalHandlerFailure("failed to parse json: ${e.message}\n$rawJson")
        }
        return NotePayload(
            json.getString("title") ?: title
                ?: throw ExternalHandlerFailure("no title supplied!\n$rawJson"),
            json.getString("content"),
            json.getString("state"),
            json.getString("priority"),
            json.getString("scheduled"),
            json.getString("deadline"),
            json.getString("closed"),
            (json.getString("tags") ?: "")
                .split(" +".toRegex())
                .filter { it.isNotEmpty() },
            OrgProperties().apply {
                json["properties"]?.asMap?.forEach { (k, v) -> this[k] = v }
            }
        )
    }

    private fun getNoteByQuery(rawQuery: String?): NoteView {
        if (rawQuery == null)
            throw ExternalHandlerFailure("couldn't find note")
        val query = InternalQueryParser().parse(rawQuery)
        val notes = dataRepository.selectNotesFromQuery(query)
        if (notes.isEmpty())
            throw ExternalHandlerFailure("couldn't find note")
        if (notes.size > 1)
            throw ExternalHandlerFailure("query \"$rawQuery\" gave multiple results")
        return notes[0]
    }

    fun Intent.getNote(prefix: String = "") =
        dataRepository.getNoteView(getLongExtra("${prefix}NOTE_ID", -1))
            ?: dataRepository.getNoteAtPath(getStringExtra("${prefix}NOTE_PATH") ?: "")
            ?: getNoteByQuery(getStringExtra("${prefix}NOTE_QUERY"))

    fun Intent.getNoteAndProps(prefix: String = "") = getNote(prefix).let {
        it to dataRepository.getNoteProperties(it.note.id)
    }

    fun Intent.getBook(prefix: String = "") =
        dataRepository.getBook(getLongExtra("${prefix}BOOK_ID", -1))
            ?: dataRepository.getBook(getStringExtra("${prefix}BOOK_NAME") ?: "")
            ?: throw ExternalHandlerFailure("couldn't find book")

    fun Intent.getNotePlace() = try {
        getNote(prefix="PARENT_").let { noteView ->
            val place = try {
                Place.valueOf(getStringExtra("PLACEMENT") ?: "")
            } catch (e: IllegalArgumentException) { Place.UNDER }
            dataRepository.getBook(noteView.bookName)?.let { book ->
                NotePlace(book.id, noteView.note.id, place)
            }
        }
    } catch (e: ExternalHandlerFailure) { null } ?: try {
        NotePlace(getBook(prefix="PARENT_").id)
    } catch (e: ExternalHandlerFailure) {
        throw ExternalHandlerFailure("couldn't find parent note/book")
    }

    fun Intent.getNoteIds(allowSingle: Boolean = true, allowEmpty: Boolean = false): Set<Long> {
        val id = if (allowSingle) getLongExtra("NOTE_ID", -1) else null
        val ids = getLongArrayExtra("NOTE_IDS")?.toTypedArray() ?: emptyArray()
        val path =
            if (allowSingle)
                getStringExtra("NOTE_PATH")
                    ?.let { dataRepository.getNoteAtPath(it)?.note?.id }
            else null
        val paths = (getStringArrayExtra("NOTE_PATHS") ?: emptyArray())
            .mapNotNull { dataRepository.getNoteAtPath(it)?.note?.id }
            .toTypedArray()
        return listOfNotNull(id, *ids, path, *paths).filter { it >= 0 }.toSet().also {
            if (it.isEmpty() && !allowEmpty)
                throw ExternalHandlerFailure("no notes specified")
        }
    }

    fun Intent.getSavedSearch() =
        dataRepository.getSavedSearch(getLongExtra("SAVED_SEARCH_ID", -1))
            ?: dataRepository.getSavedSearches()
                .find { it.name == getStringExtra("SAVED_SEARCH_NAME") }
            ?: throw ExternalHandlerFailure("couldn't find saved search")

    fun Intent.getNewSavedSearch(allowBlank: Boolean = false): SavedSearch {
        val name = getStringExtra("SAVED_SEARCH_NEW_NAME")
        val query = getStringExtra("SAVED_SEARCH_NEW_QUERY")
        if (!allowBlank && (name.isNullOrBlank() || query.isNullOrBlank()))
            throw ExternalHandlerFailure("invalid parameters for new saved search")
        return SavedSearch(0, name ?: "", query ?: "", 0)
    }

    private fun JsonObject.getString(name: String) = this[name]?.let {
        if (it.isJsonPrimitive && it.asJsonPrimitive.isString)
            it.asJsonPrimitive.asString
        else null
    }

    private val JsonElement.asMap: Map<String, String>?
        get() = if (this.isJsonObject) {
            this.asJsonObject
                .entrySet()
                .map {
                    if (it.value.isJsonPrimitive)
                        it.key to it.value.asJsonPrimitive.asString
                    else return null
                }
                .toMap()
        } else null
}
