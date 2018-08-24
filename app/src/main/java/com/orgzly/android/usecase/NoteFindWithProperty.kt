package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository

class NoteFindWithProperty(val name: String, val value: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        val note = dataRepository.findNoteHavingProperty(name, value)

        return UseCaseResult(
                userData = note
        )
    }
}