package com.orgzly.android.usecase

import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.BookView

class BookRename(val bookView: BookView, val name: String) : UseCase() {
    override fun run(dataRepository: DataRepository): UseCaseResult {
        dataRepository.renameBook(bookView, name)

        return UseCaseResult(
                modifiesLocalData = true,
                triggersSync = SYNC_DATA_MODIFIED
        )
    }
}