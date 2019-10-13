package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteUpdateState(val noteIds: Set<Long>, val state: String?) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setNotesState(noteIds, state)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}