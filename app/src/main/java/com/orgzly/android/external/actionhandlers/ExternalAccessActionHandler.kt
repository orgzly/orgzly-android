package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.SavedSearch
import com.orgzly.android.external.types.Response
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.org.OrgProperties
import javax.inject.Inject

abstract class ExternalAccessActionHandler {
    @Inject
    lateinit var dataRepository: DataRepository

    init {
        @Suppress("LeakingThis")
        App.appComponent.inject(this)
    }

    abstract val actions: List<List<Pair<String, (Intent, Context) -> Response>>>
    private val fullNameActions by lazy {
        actions.flatten().toMap().mapKeys { (key, _) -> "com.orgzly.android.$key" }
    }

    fun Intent.getNotePayload(title: String? = null): NotePayload? {
        val rawJson = getStringExtra("NOTE_PAYLOAD")
        val json = try {
            JsonParser.parseString(rawJson)
                    .let { if (it.isJsonObject) it.asJsonObject else null }
        } catch (e: JsonParseException) {
            null
        }

        return try {
            json!!
            NotePayload(
                    (json.getString("title") ?: title)!!,
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
        } catch (e: NullPointerException) { null }
    }

    fun Intent.getNotePlace() = getNote(prefix="PARENT_")?.let { noteView ->
        val place = try {
            Place.valueOf(getStringExtra("PLACEMENT") ?: "")
        } catch (e: IllegalArgumentException) { Place.UNDER }
        dataRepository.getBook(noteView.bookName)?.let { book ->
            NotePlace(book.id, noteView.note.id, place)
        }
    } ?: getBook(prefix="PARENT")?.let { book -> NotePlace(book.id) }

    fun Intent.getNoteIds(allowSingle: Boolean = true): Set<Long> {
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
        return listOfNotNull(id, *ids, path, *paths).filter { it >= 0 }.toSet()
    }

    fun Intent.getNote(prefix: String = "") =
            dataRepository.getNoteView(getLongExtra("${prefix}NOTE_ID", -1))
                    ?: dataRepository.getNoteAtPath(getStringExtra("${prefix}NOTE_PATH") ?: "")

    fun Intent.getBook(prefix: String = "") =
            dataRepository.getBook(getLongExtra("${prefix}BOOK_ID", -1))
                    ?: dataRepository.getBook(getStringExtra("${prefix}BOOK_NAME") ?: "")

    fun Intent.getSavedSearch() =
            dataRepository.getSavedSearch(getLongExtra("SAVED_SEARCH_ID", -1))
                    ?: dataRepository.getSavedSearches()
                            .find { it.name == getStringExtra("SAVED_SEARCH_NAME") }

    fun Intent.getNewSavedSearch(allowBlank: Boolean = false): SavedSearch? {
        val name = getStringExtra("SAVED_SEARCH_NEW_NAME")
        val query = getStringExtra("SAVED_SEARCH_NEW_QUERY")
        if (!allowBlank && (name.isNullOrBlank() || query.isNullOrBlank())) return null
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

    fun action(f: (Intent, Context) -> Response, vararg names: String) = names.map { it to f }

    @JvmName("intentAction")
    fun action(f: (Intent) -> Response, vararg names: String) =
            action({ i, _ -> f(i) }, *names)

    @JvmName("contextAction")
    fun action(f: (Context) -> Response, vararg names: String) =
            action({ _, c -> f(c) }, *names)

    fun action(f: () -> Response, vararg names: String) =
            action({ _, _ -> f() }, *names)


    fun handle(intent: Intent, context: Context) =
            fullNameActions[intent.action!!]?.let { it(intent, context) }
}