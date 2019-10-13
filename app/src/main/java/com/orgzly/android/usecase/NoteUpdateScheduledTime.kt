package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.org.datetime.OrgDateTime

class NoteUpdateScheduledTime(val noteIds: Set<Long>, val time: OrgDateTime?) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.setNotesScheduledTime(noteIds, time)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}