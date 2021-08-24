package com.orgzly.android.external.actionhandlers

import android.content.Intent
import com.orgzly.android.external.types.Response

class EditNotes : ExternalAccessActionHandler() {
    override val actions = listOf(
            action(::addNote, "ADD_NOTE"),
            action(::editNote, "EDIT_NOTE"),
            action(::refileNote, "REFILE_NOTE", "REFILE_NOTES"),
            action(::moveNote, "MOVE_NOTE", "MOVE_NOTES"),
            action(::deleteNote, "DELETE_NOTE", "DELETE_NOTES")
    )

    private fun addNote(intent: Intent): Response {
        val place = intent.getNotePlace()
                ?: return Response(false, "Could not find parent note/book")
        val newNote = intent.getNotePayload()
                ?: return Response(false, "Invalid payload")
        val note = dataRepository.createNote(newNote, place)
        return Response(true, "${note.id}")
    }

    private fun editNote(intent: Intent): Response {
        val noteView = intent.getNote()
                ?: return Response(false, "Couldn't find note")
        val newNote = intent.getNotePayload(title=noteView.note.title)
                ?: return Response(false, "Invalid payload")
        dataRepository.updateNote(noteView.note.id, newNote)
        return Response()
    }

    private fun refileNote(intent: Intent): Response {
        val notes = intent.getNoteIds()
        if (notes.isEmpty())
            return Response(false, "No notes specified")
        val place = intent.getNotePlace()
                ?: return Response(false, "Couldn't find note")
        dataRepository.refileNotes(notes, place)
        return Response()
    }

    private fun moveNote(intent: Intent): Response {
        val notes = intent.getNoteIds()
        if (notes.isEmpty()) return Response(false, "No notes specified")
        with(dataRepository) { when (intent.getStringExtra("DIRECTION")) {
            "UP" -> intent.getBook()?.id?.let { moveNote(it, notes, -1) }
            "DOWN" -> intent.getBook()?.id?.let { moveNote(it, notes, 1) }
            "LEFT" -> promoteNotes(notes)
            "RIGHT" -> demoteNotes(notes)
            else -> return Response(false, "Invalid direction")
        } }
        return Response()
    }

    private fun deleteNote(intent: Intent): Response {
        val book = intent.getBook() ?: return Response(false, "Couldn't find specified book")
        val notes = intent.getNoteIds()
        if (notes.isEmpty()) return Response(false, "No notes specified")
        dataRepository.deleteNotes(book.id, notes)
        return Response()
    }
}