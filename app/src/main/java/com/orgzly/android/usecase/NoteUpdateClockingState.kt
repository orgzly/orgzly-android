package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteUpdateClockingState(val noteIds: Set<Long>, val type: Int) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setNotesClockingState(noteIds, type)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}