package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteUpdateStateDone(val noteId: Long) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setNoteStateToDone(noteId)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}