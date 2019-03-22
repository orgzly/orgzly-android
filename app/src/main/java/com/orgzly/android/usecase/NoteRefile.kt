package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.NotePlace

class NoteRefile(val noteIds: Set<Long>, val target: NotePlace) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.refileNotes(noteIds, target)

        val firstRefilledNote = dataRepository.getFirstNote(noteIds)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = UseCase.SYNC_DATA_MODIFIED,
                userData = firstRefilledNote
        )
    }
}