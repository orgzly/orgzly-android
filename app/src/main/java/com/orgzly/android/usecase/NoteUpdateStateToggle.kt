package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

/**
 * Sets the state of notes to the first done-type state defined in Settings.
 */
class NoteUpdateStateToggle(val noteIds: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.toggleNotesState(noteIds)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}