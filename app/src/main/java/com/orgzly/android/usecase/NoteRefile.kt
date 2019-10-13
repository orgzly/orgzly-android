package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.ui.NotePlace

class NoteRefile(val noteIds: Set<Long>, val target: NotePlace) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        checkIfValidTarget(dataRepository, target)

        dataRepository.refileNotes(noteIds, target)

        val firstRefilledNote = dataRepository.getFirstNote(noteIds)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED,
                userData = firstRefilledNote
        )
    }

    /**
     * Make sure there is no overlap - notes can't be refiled under themselves
     */
    private fun checkIfValidTarget(dataRepository: DataRepository, notePlace: NotePlace) {
        if (notePlace.noteId != 0L) {
            val sourceNotes = dataRepository.getNotesAndSubtrees(noteIds)

            if (sourceNotes.map { it.id }.contains(notePlace.noteId)) {
                throw TargetInNotesSubtree()
            }
        }
    }

    class TargetInNotesSubtree : Throwable()
}