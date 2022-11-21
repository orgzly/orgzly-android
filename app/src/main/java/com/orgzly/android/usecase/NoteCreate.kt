package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.NotePlace
import com.orgzly.android.ui.note.NotePayload

class NoteCreate(val notePayload: NotePayload, val notePlace: NotePlace) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val note = dataRepository.createNote(notePayload, notePlace)

        if (notePayload.attachmentUri != null) {
            dataRepository.storeAttachment(
                    notePlace.bookId,
                    notePayload)
        }

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_NOTE_CREATED,
                userData = note)
    }
}