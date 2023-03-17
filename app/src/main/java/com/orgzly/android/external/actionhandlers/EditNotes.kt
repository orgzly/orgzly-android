package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.orgzly.android.external.types.ExternalHandlerFailure

class EditNotes : ExternalAccessActionHandler() {
    override val actions = listOf(
            action(::addNote, "ADD_NOTE"),
            action(::editNote, "EDIT_NOTE"),
            action(::refileNote, "REFILE_NOTE", "REFILE_NOTES"),
            action(::moveNote, "MOVE_NOTE", "MOVE_NOTES"),
            action(::deleteNote, "DELETE_NOTE", "DELETE_NOTES")
    )

    private fun addNote(intent: Intent): String {
        val place = intent.getNotePlace()
        val newNote = intent.getNotePayload()
        val note = dataRepository.createNote(newNote, place)
        return "${note.id}"
    }

    private fun editNote(intent: Intent) {
        val noteView = intent.getNote()
        val newNote = intent.getNotePayload(title=noteView.note.title)
        dataRepository.updateNote(noteView.note.id, newNote)
    }

    private fun refileNote(intent: Intent) {
        val notes = intent.getNoteIds()
        val place = intent.getNotePlace()
        dataRepository.refileNotes(notes, place)
    }

    private fun moveNote(intent: Intent) {
        val notes = intent.getNoteIds()
        with(dataRepository) { when (intent.getStringExtra("DIRECTION")) {
            "UP" -> moveNote(intent.getBook().id, notes, -1)
            "DOWN" -> moveNote(intent.getBook().id, notes, 1)
            "LEFT" -> promoteNotes(notes)
            "RIGHT" -> demoteNotes(notes)
            else -> throw ExternalHandlerFailure("invalid direction")
        } }
    }

    private fun deleteNote(intent: Intent) {
        intent.getNoteIds().groupBy {
            dataRepository.getNoteView(it)?.bookName
                    ?: throw ExternalHandlerFailure("invalid note id $it")
        }.forEach { (bookName, notes) ->
            dataRepository.deleteNotes(dataRepository.getBook(bookName)!!.id, notes.toSet())
        }
    }
}
