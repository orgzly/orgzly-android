package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NotePromote(val noteIds: Set<Long>) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val count = dataRepository.promoteNotes(noteIds)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED,
                userData = count
        )
    }
}