package com.orgzly.android.external.actionhandlers

import android.content.Context
import android.content.Intent
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.orgzly.android.external.types.Response
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.Place
import com.orgzly.android.ui.note.NotePayload
import com.orgzly.org.OrgProperties

class EditNotes : ExternalAccessActionHandler() {
    override val actions = listOf(
        action(::addNote, "ADD_NOTE", "ADD_NOTES"),
        action(::deleteNote, "DELETE_NOTE", "DELETE_NOTES")
    )

    fun addNote(intent: Intent): Response {
        val book = getBook(intent) ?: return Response(false, "Couldn't find specified book")
        val newNote = notePayloadFromJson(intent.getStringExtra("PAYLOAD") ?: "")
                ?: return Response(false, "Invalid payload")
        val path = intent.getStringExtra("PATH") ?: ""

        val place = if (path.split("/").any { it.isNotEmpty() }) {
            dataRepository.getNoteAtPath(book.name, path)?.let {
                NotePlace(book.id, it.note.id, Place.UNDER)
            }
        } else null
        place ?: return Response(false, "Couldn't find parent note at path")
        dataRepository.createNote(newNote, place)
        return Response(true, null)
    }

    // <editor-fold desc="Helpers">

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

    private fun notePayloadFromJson(rawJson: String): NotePayload? {
        val json = try {
            JsonParser.parseString(rawJson)
                    .let { if (it.isJsonObject) it.asJsonObject else null }
        } catch (e: JsonParseException) {
            null
        }
        return try {
            json!!
            NotePayload(
                    json.getString("title")!!,
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

    // </editor-fold>
}