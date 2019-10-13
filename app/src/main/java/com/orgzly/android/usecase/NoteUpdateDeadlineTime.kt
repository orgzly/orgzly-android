package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.org.datetime.OrgDateTime

class NoteUpdateDeadlineTime(val noteIds: Set<Long>, val time: OrgDateTime?) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setNotesDeadlineTime(noteIds, time)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}