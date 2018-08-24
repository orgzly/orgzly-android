package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.note.NotePayload

class NoteUpdate(val noteId: Long, val notePayload: NotePayload) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.updateNote(noteId, notePayload)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED
        )
    }
}